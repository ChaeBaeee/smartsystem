package com.smartstudy.models

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val color: String = "#3498db", // Default blue color
    val targetHoursPerWeek: Double = 10.0,
    val description: String = ""
)

