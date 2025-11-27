package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: String,
    val subjectId: String,
    val name: String,
    val lastReviewed: Long? = null, // Unix timestamp in milliseconds
    val reviewCount: Int = 0,
    val difficulty: Int = 2, // 1 = Easy, 2 = Medium, 3 = Hard
    val manualPriority: Float? = null, // Manual priority override (0-10)
    val notes: String = ""
)

