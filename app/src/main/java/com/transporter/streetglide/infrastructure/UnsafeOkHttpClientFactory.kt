package com.transporter.streetglide.infrastructure

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * UnsafeOkHttpClientFactory
 * FIXME: To be moved to testing only
 */
object UnsafeOkHttpClientFactory {
    private val trustAllCerts = object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message -> println(message) } // Remove me
        logging.level = HttpLoggingInterceptor.Level.BODY // Remove me

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustAllCerts), java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(logging) // Remove me
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .build()

    }
}