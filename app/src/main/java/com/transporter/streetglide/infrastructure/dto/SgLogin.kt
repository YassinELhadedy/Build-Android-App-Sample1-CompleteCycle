package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.infrastructure.InfrastructureException
import com.transporter.streetglide.models.User

/**
 * SgLogin
 */
data class SgLogin(val username: String,
                   val password: String) {
    companion object {
        fun User.toSgLogin() = if (username == null || password == null) {
            throw InfrastructureException("User username and password are needed!")
        } else {
            SgLogin(username, password)
        }
    }
}