package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.models.GetRepository
import com.transporter.streetglide.models.User
import io.reactivex.Observable

/**
 * UserSuperGlideRepository
 */
class UserSuperGlideRepository(private val superGlideRestService: SuperGlideRestService, private val configurationRepository: ConfigurationRepository) : GetRepository<User> {

    override fun get(id: Int): Observable<out User> {
        return configurationRepository.get(13).flatMap {
            superGlideRestService.getUser(it.signedToken.toGlider(), id)
                    .map { it.info.toUser() }
        }
    }
}