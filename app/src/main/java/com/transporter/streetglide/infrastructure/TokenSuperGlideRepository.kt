package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.infrastructure.dto.SgLogin.Companion.toSgLogin
import com.transporter.streetglide.models.PostRepository
import com.transporter.streetglide.models.SignedToken
import com.transporter.streetglide.models.User
import io.reactivex.Observable

/**
 * TokenSuperGlideRepository
 * FIXME: Maybe should be removed!
 */
class TokenSuperGlideRepository(private var superGlideRestService: SuperGlideRestService) : PostRepository<User, SignedToken> {
    override fun insert(entity: User): Observable<out SignedToken> {
        return superGlideRestService.login(entity.toSgLogin()).map {
            it.toSignedToken()
        }
    }
}