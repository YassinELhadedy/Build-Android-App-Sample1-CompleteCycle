package com.transporter.streetglide.models

import com.auth0.android.jwt.JWT

/**
 * Created by mcherri on 11/29/17.
 * SignedToken
 */
data class SignedToken(val token: String) {

    private fun newJwt() = JWT(token)

    fun toGlider() = "Glider" + token

    val id: Int
        get() = newJwt().getClaim("id").asInt() ?:
                throw ModelException("JWT token does not have an id claim!")

    val isExpired: Boolean
        get() = newJwt().isExpired(0)

    val name: String
        get() = newJwt().getClaim("name").asString() ?:
                throw ModelException("JWT token does not have a name claim!")

    val username: String
        get() = newJwt().getClaim("username").asString() ?:
                throw ModelException("JWT token does not have a username claim!")

    val type: Int // FIXME: Should be a type.
        get() = newJwt().getClaim("type").asInt() ?:
                throw ModelException("JWT token does not have a type claim!")
}