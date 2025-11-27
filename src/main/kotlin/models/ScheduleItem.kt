package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleItem(
    val id: String,
    val subjectId: String,
    val dayOfWeek: Int, // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    val startTime: String, // Format: "HH:mm" (e.g., "14:30")
    val durationMinutes: Int,
    val recurring: Boolean = true,
    val topic: String = "",
    val enabled: Boolean = true
)

