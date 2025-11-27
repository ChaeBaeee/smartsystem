package com.smartstudy.services

import com.smartstudy.data.DataManager
import com.smartstudy.models.ScheduleItem
import com.smartstudy.models.StudySession
import com.smartstudy.models.Subject
import com.smartstudy.models.Topic
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

class StudyScheduleService {
    
    // Time slot representation (hour, minute)
    data class TimeSlot(val day: Int, val hour: Int, val minute: Int = 0) {
        fun toTimeString(): String = String.format("%02d:%02d", hour, minute)
        
        fun overlaps(other: TimeSlot, durationMinutes: Int, otherDurationMinutes: Int): Boolean {
            if (day != other.day) return false
            val thisStart = hour * 60 + minute
            val thisEnd = thisStart + durationMinutes
            val otherStart = other.hour * 60 + other.minute
            val otherEnd = otherStart + otherDurationMinutes
            return thisStart < otherEnd && otherStart < thisEnd
        }
    }
    
    // Available time slots for scheduling (8am to 9pm)
    private val availableHours = listOf(8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20)
    
    /**
     * Analyze study patterns and generate adaptive schedule based on difficulty
     */
    fun generateAdaptiveSchedule(): List<ScheduleItem> {
        val subjects = DataManager.getSubjects()
        val topics = DataManager.getTopics()
        
        if (subjects.isEmpty()) return emptyList()
        
        val schedule = mutableListOf<ScheduleItem>()
        val usedSlots = mutableListOf<Pair<TimeSlot, Int>>() // Slot and duration
        val sessionsPerDay = mutableMapOf<Int, Int>() // Track how many sessions per day
        
        // Calculate average difficulty per subject based on topics
        val subjectDifficulty = subjects.associateWith { subject ->
            val subjectTopics = topics.filter { it.subjectId == subject.id }
            if (subjectTopics.isEmpty()) 5.0 else subjectTopics.map { it.difficulty }.average()
        }
        
        // Sort subjects by difficulty (harder subjects get scheduled first for better slots)
        val sortedSubjects = subjects.sortedByDescending { subjectDifficulty[it] ?: 5.0 }
        
        // Track topics to schedule for each subject
        val topicsBySubject = topics.groupBy { it.subjectId }
        
        // Calculate total sessions needed
        val allSessionsNeeded = mutableListOf<Triple<Subject, Int, Int>>() // Subject, duration, topic index
        
        sortedSubjects.forEach { subject ->
            val targetMinutes = (subject.targetHoursPerWeek * 60).toInt()
            val avgDifficulty = subjectDifficulty[subject] ?: 5.0
            
            val sessionDuration = when {
                avgDifficulty >= 7 -> 90  // Hard: 90 min sessions
                avgDifficulty >= 4 -> 60  // Medium: 60 min sessions
                else -> 45                 // Easy: 45 min sessions
            }
            
            // Limit sessions per week (max 4 per subject to leave free time)
            val sessionsPerWeek = (targetMinutes / sessionDuration).coerceIn(1, 4)
            
            repeat(sessionsPerWeek) { index ->
                allSessionsNeeded.add(Triple(subject, sessionDuration, index))
            }
        }
        
        // Spread sessions across the week with gaps
        // Use alternating days pattern: 0, 2, 4, 6 (Sun, Tue, Thu, Sat) or 1, 3, 5 (Mon, Wed, Fri)
        val pattern1 = listOf(1, 3, 5) // Mon, Wed, Fri
        val pattern2 = listOf(0, 2, 4, 6) // Sun, Tue, Thu, Sat
        val allDays = (pattern1 + pattern2).distinct()
        
        // Max 2 sessions per day to avoid overload
        val maxSessionsPerDay = 2
        
        // Distribute sessions evenly
        var dayIndex = 0
        allSessionsNeeded.forEach { (subject, sessionDuration, topicIndex) ->
            val avgDifficulty = subjectDifficulty[subject] ?: 5.0
            val subjectTopics = topicsBySubject[subject.id]?.sortedByDescending { it.difficulty } ?: emptyList()
            
            // Find a day that hasn't reached max sessions
            var attempts = 0
            var scheduled = false
            
            while (attempts < allDays.size && !scheduled) {
                val day = allDays[(dayIndex + attempts) % allDays.size]
                val currentDaySessions = sessionsPerDay.getOrDefault(day, 0)
                
                if (currentDaySessions < maxSessionsPerDay) {
                    val availableSlot = findAvailableSlot(day, sessionDuration, usedSlots, avgDifficulty)
                    
                    if (availableSlot != null) {
                        val topicName = subjectTopics.getOrNull(topicIndex % subjectTopics.size.coerceAtLeast(1))?.name ?: ""
                        
                        val scheduleItem = ScheduleItem(
                            id = UUID.randomUUID().toString(),
                            subjectId = subject.id,
                            dayOfWeek = day,
                            startTime = availableSlot.toTimeString(),
                            durationMinutes = sessionDuration,
                            topic = topicName,
                            recurring = true,
                            enabled = true
                        )
                        
                        schedule.add(scheduleItem)
                        usedSlots.add(Pair(availableSlot, sessionDuration))
                        sessionsPerDay[day] = currentDaySessions + 1
                        scheduled = true
                        dayIndex = (dayIndex + 2) % allDays.size // Skip to create gaps
                    }
                }
                attempts++
            }
        }
        
        return schedule.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }
    
    /**
     * Find an available time slot that doesn't conflict with existing slots
     */
    private fun findAvailableSlot(
        day: Int, 
        durationMinutes: Int, 
        usedSlots: List<Pair<TimeSlot, Int>>,
        difficulty: Double
    ): TimeSlot? {
        // For harder subjects, prefer morning/early afternoon (better focus)
        // For easier subjects, afternoon/evening is fine
        val preferredHours = if (difficulty >= 6) {
            listOf(9, 10, 11, 14, 15, 8, 16, 17) // Morning/early afternoon first
        } else {
            listOf(14, 15, 16, 17, 18, 19, 10, 11) // Afternoon/evening first
        }
        
        for (hour in preferredHours) {
            if (hour !in availableHours) continue
            
            val slot = TimeSlot(day, hour)
            val hasConflict = usedSlots.any { (existingSlot, existingDuration) ->
                slot.overlaps(existingSlot, durationMinutes, existingDuration)
            }
            
            if (!hasConflict) {
                return slot
            }
        }
        
        // Try all available hours as fallback
        for (hour in availableHours.shuffled()) {
            val slot = TimeSlot(day, hour)
            val hasConflict = usedSlots.any { (existingSlot, existingDuration) ->
                slot.overlaps(existingSlot, durationMinutes, existingDuration)
            }
            
            if (!hasConflict) {
                return slot
            }
        }
        
        return null
    }
    
    /**
     * Analyze when user typically studies for each subject
     */
    private fun analyzeTimePatterns(sessions: List<StudySession>): Map<String, List<Int>> {
        val patterns = mutableMapOf<String, MutableList<Int>>()
        
        sessions.filter { it.endTime != null }.forEach { session ->
            val hour = java.util.Calendar.getInstance().apply {
                timeInMillis = session.startTime
            }.get(java.util.Calendar.HOUR_OF_DAY)
            
            patterns.getOrPut(session.subjectId) { mutableListOf() }.add(hour)
        }
        
        // Return most common hours for each subject
        return patterns.mapValues { (_, hours) ->
            hours.groupingBy { it }.eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
        }
    }
    
    /**
     * Update schedule based on actual study patterns
     */
    fun updateScheduleFromPatterns() {
        val newSchedule = generateAdaptiveSchedule()
        val existing = DataManager.getScheduleItems()
        
        // Remove old schedule items
        existing.forEach { DataManager.deleteScheduleItem(it.id) }
        
        // Add new adaptive schedule
        newSchedule.forEach { DataManager.addScheduleItem(it) }
    }
    
    /**
     * Get schedule items for a specific day
     */
    fun getScheduleForDay(dayOfWeek: Int): List<ScheduleItem> {
        return DataManager.getScheduleItems()
            .filter { it.dayOfWeek == dayOfWeek && it.enabled }
            .sortedBy { it.startTime }
    }
    
    /**
     * Get statistics about study progress per subject
     * Returns map of subject ID to percentage of target achieved
     */
    fun getSubjectProgress(): Map<String, Double> {
        val subjects = DataManager.getSubjects()
        val sessions = DataManager.getStudySessions()
        
        return subjects.associate { subject ->
            val totalMinutes = sessions
                .filter { it.subjectId == subject.id && it.endTime != null }
                .sumOf { it.durationMinutes }
            val targetMinutes = (subject.targetHoursPerWeek * 60).toInt()
            val percentage = if (targetMinutes > 0) {
                (totalMinutes.toDouble() / targetMinutes * 100).coerceAtMost(100.0)
            } else 0.0
            subject.id to percentage
        }
    }
}

