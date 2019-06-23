package com.transporter.streetglide.infrastructure

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * SuperGlideRestServiceFactory
 */
class SuperGlideRestServiceFactory(private val baseUrl: String) {

    val service: SuperGlideRestService by lazy {
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(UnsafeOkHttpClientFactory.okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SuperGlideRestService::class.java)
    }
}