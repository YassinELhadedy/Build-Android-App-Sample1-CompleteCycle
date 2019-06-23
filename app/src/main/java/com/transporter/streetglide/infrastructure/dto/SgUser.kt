package com.transporter.streetglide.infrastructure.dto

import com.transporter.streetglide.models.User

/**
 * Created by yassin on 10/18/17.
 * SgUser
 */
data class SgUser(val name: String,
                  val username: String?,
                  val password: String?,
                  val isActive: Boolean,
                  val runnerCode: String?) {
    companion object {
        fun User.toSgUser(): SgUser = SgUser(name,
                username,
                password,
                isActive,
                runnerCode)
    }

    fun toUser(): User {
        return User(name,
                username,
                password,
                isActive,
                runnerCode)
    }
}