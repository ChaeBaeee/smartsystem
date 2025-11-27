package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class PerformanceAlert(
    val id: String,
    val type: AlertType,
    val subjectId: String? = null,
    val message: String,
    val date: Long, // Unix timestamp in milliseconds
    val resolved: Boolean = false,
    val severity: Int = 1 // 1 = Low, 2 = Medium, 3 = High
)

@Serializable
enum class AlertType {
    GRADE_DROP,
    LOW_ATTENDANCE,
    MISSED_STUDY_GOAL,
    PERFORMANCE_DECLINE,
    GENERAL
}

