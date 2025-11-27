package com.smartstudy.services

import com.smartstudy.data.DataManager
import com.smartstudy.models.Subject
import com.smartstudy.models.StudySession
import java.util.*

class ProgressTrackingService {
    
    /**
     * Get progress for a specific subject
     */
    fun getSubjectProgress(subjectId: String): SubjectProgress {
        val subject = DataManager.getSubjects().find { it.id == subjectId }
            ?: return SubjectProgress(subjectId, 0.0, 0, 0, emptyList())
        
        val sessions = DataManager.getStudySessions()
            .filter { it.subjectId == subjectId && it.endTime != null }
        
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val targetMinutes = subject.targetHoursPerWeek * 60
        val completionPercentage = if (targetMinutes > 0) {
            (totalMinutes.toDouble() / targetMinutes * 100).coerceAtMost(100.0)
        } else 0.0
        
        // Calculate weekly progress
        val calendar = Calendar.getInstance()
        val weekStart = calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val thisWeekMinutes = sessions
            .filter { it.startTime >= weekStart }
            .sumOf { it.durationMinutes }
        
        // Trend analysis (last 4 weeks)
        val weeklyTrends = getWeeklyTrends(subjectId, 4)
        
        return SubjectProgress(
            subjectId = subjectId,
            completionPercentage = completionPercentage,
            totalMinutes = totalMinutes,
            thisWeekMinutes = thisWeekMinutes,
            weeklyTrends = weeklyTrends
        )
    }
    
    /**
     * Get progress for all subjects
     */
    fun getAllSubjectProgress(): List<SubjectProgress> {
        return DataManager.getSubjects().map { getSubjectProgress(it.id) }
    }
    
    /**
     * Get weekly trends for a subject
     */
    fun getWeeklyTrends(subjectId: String, weeks: Int = 4): List<WeeklyData> {
        val sessions = DataManager.getStudySessions()
            .filter { it.subjectId == subjectId && it.endTime != null }
        
        val calendar = Calendar.getInstance()
        val trends = mutableListOf<WeeklyData>()
        
        repeat(weeks) { weekOffset ->
            val weekStart = calendar.clone() as Calendar
            weekStart.add(Calendar.WEEK_OF_YEAR, -weekOffset)
            weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            weekStart.set(Calendar.HOUR_OF_DAY, 0)
            weekStart.set(Calendar.MINUTE, 0)
            weekStart.set(Calendar.SECOND, 0)
            weekStart.set(Calendar.MILLISECOND, 0)
            
            val weekEnd = weekStart.clone() as Calendar
            weekEnd.add(Calendar.DAY_OF_WEEK, 6)
            weekEnd.set(Calendar.HOUR_OF_DAY, 23)
            weekEnd.set(Calendar.MINUTE, 59)
            
            val weekMinutes = sessions
                .filter { 
                    it.startTime >= weekStart.timeInMillis && 
                    it.startTime <= weekEnd.timeInMillis 
                }
                .sumOf { it.durationMinutes }
            
            trends.add(WeeklyData(
                weekStart = weekStart.timeInMillis,
                minutes = weekMinutes
            ))
        }
        
        return trends.reversed()
    }
    
    /**
     * Identify weak areas (subjects with low progress)
     */
    fun identifyWeakAreas(threshold: Double = 50.0): List<String> {
        return getAllSubjectProgress()
            .filter { it.completionPercentage < threshold }
            .map { it.subjectId }
    }
    
    /**
     * Get overall progress statistics
     */
    fun getOverallStatistics(): OverallStatistics {
        val allProgress = getAllSubjectProgress()
        val totalMinutes = allProgress.sumOf { it.totalMinutes }
        val averageCompletion = if (allProgress.isNotEmpty()) {
            allProgress.map { it.completionPercentage }.average()
        } else 0.0
        
        val improvingSubjects = allProgress.count { progress ->
            val trends = progress.weeklyTrends
            if (trends.size >= 2) {
                trends.last().minutes > trends[trends.size - 2].minutes
            } else false
        }
        
        return OverallStatistics(
            totalStudyMinutes = totalMinutes,
            averageCompletion = averageCompletion,
            improvingSubjects = improvingSubjects,
            totalSubjects = allProgress.size
        )
    }
    
    data class SubjectProgress(
        val subjectId: String,
        val completionPercentage: Double,
        val totalMinutes: Int,
        val thisWeekMinutes: Int,
        val weeklyTrends: List<WeeklyData>
    )
    
    data class WeeklyData(
        val weekStart: Long,
        val minutes: Int
    )
    
    data class OverallStatistics(
        val totalStudyMinutes: Int,
        val averageCompletion: Double,
        val improvingSubjects: Int,
        val totalSubjects: Int
    )
}

