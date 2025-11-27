package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class Attendance(
    val id: String,
    val subjectId: String,
    val date: Long, // Unix timestamp in milliseconds
    val present: Boolean = true,
    val notes: String = ""
)

