package com.transporter.streetglide.models

data class User(val name: String,
                val username: String?,
                val password: String?,
                val isActive: Boolean,
                val runnerCode: String?)