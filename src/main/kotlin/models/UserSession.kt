package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: String? = null,
    val loggedIn: Boolean = false,
    val lastLogin: Long? = null
)

