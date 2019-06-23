package com.transporter.streetglide.models.services

import com.transporter.streetglide.infrastructure.ConfigurationRepository
import com.transporter.streetglide.infrastructure.UserSuperGlideRepository
import com.transporter.streetglide.models.Configuration
import com.transporter.streetglide.models.ModelException
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.User
import com.transporter.streetglide.models.exception.TokenExpiredException
import com.transporter.streetglide.models.exception.UserNotFoundException
import io.reactivex.Observable
import retrofit2.HttpException

/**
 *Created by yassin on 6/13/18.
 */
class GetRunnerIdService(private val userSuperGlideRepository: UserSuperGlideRepository, private val configurationRepository: ConfigurationRepository) {
    fun getRunnerId(userId: Int): Observable<out User> {
        return userSuperGlideRepository.get(userId).filter { user ->
            user.isActive && user.runnerCode != null
        }.flatMap { runner ->
            configurationRepository.update(Configuration(SignedToken(""), 0, runner.runnerCode!!))
                    .map { runner }
                    .onErrorReturn { runner }

        }.map { it }.onErrorResumeNext { e: Throwable ->
            if (e is HttpException && e.response().code() == 404) {
                Observable.error(UserNotFoundException(e))
            } else if (e is HttpException && e.response().code() == 401) {
                Observable.error(TokenExpiredException(e))
            } else {
                Observable.error(ModelException(e))
            }
        }
    }
}