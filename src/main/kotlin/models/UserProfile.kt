package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val avatarInitials: String = "",
    val institution: String = "Smart Study Academy",
    val studyGoalHours: Int = 15,
    val streakDays: Int = 0,
    val lastLogin: Long? = null
)

