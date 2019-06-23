package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.models.SignedToken

/**
 * Created by yassin on 10/12/17.
 * SgSignedToken
 */
data class SgSignedToken(val token: String) {
    companion object {
        fun SignedToken.toSgSignedToken(): SgSignedToken =
                SgSignedToken(token)
    }

    fun toSignedToken(): SignedToken = SignedToken(token)
}