package com.transporter.streetglide.models.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.TokenSuperGlideRepository
import com.transporter.streetglide.models.Configuration
import com.transporter.streetglide.models.ModelException
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.User
import com.transporter.streetglide.models.exception.UnauthorizedException
import io.reactivex.Observable
import retrofit2.HttpException

class LoginService(private val tokenRepository: TokenSuperGlideRepository, private val configurationRepository: ConfigurationRepository) {
    fun login(username: String, password: String): Observable<out SignedToken> {
        return tokenRepository.insert(User("", username, password, true, ""))
                .map { it }
                .onErrorResumeNext { e: Throwable ->
                    if (e is HttpException && e.response().code() == 401) {
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val myMap = Gson().fromJson<Map<String, Any>>(e.response().errorBody()?.byteStream()?.reader(), type)
                        val error: Map<String, Any> = myMap["errors"] as Map<String, Any>
                        val message: String = error["message"].toString()
                        Observable.error(UnauthorizedException(message))
                    } else {
                        Observable.error(ModelException(e))
                    }
                }
    }
}