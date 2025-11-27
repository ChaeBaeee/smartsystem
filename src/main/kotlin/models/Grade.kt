package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class Grade(
    val id: String,
    val subjectId: String,
    val type: String, // Renamed from assignmentName to type
    val score: Double, // Out of 100 or raw score
    val maxScore: Double = 100.0,
    val date: Long, // Unix timestamp in milliseconds
    val category: String = "" // e.g., "Exam", "Quiz", "Homework"
)

