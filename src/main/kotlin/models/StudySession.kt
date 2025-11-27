package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class StudySession(
    val id: String,
    val subjectId: String,
    val topic: String = "",
    val startTime: Long, // Unix timestamp in milliseconds
    val endTime: Long? = null, // null if session is ongoing
    val durationMinutes: Int = 0, // Calculated or manually entered
    val notes: String = ""
)

