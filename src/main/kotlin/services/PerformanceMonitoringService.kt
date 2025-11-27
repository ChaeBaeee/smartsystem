package com.smartstudy.services

import com.smartstudy.data.DataManager
import com.smartstudy.models.Grade
import java.util.*

class PerformanceMonitoringService {
    
    /**
     * Get average grade for a subject
     */
    fun getAverageGrade(subjectId: String): Double? {
        val grades = DataManager.getGrades().filter { it.subjectId == subjectId }
        if (grades.isEmpty()) return null
        return grades.map { it.score / it.maxScore * 100 }.average()
    }
    
    /**
     * Get grade trends for a subject
     */
    fun getGradeTrends(subjectId: String, limit: Int = 10): List<GradeDataPoint> {
        return DataManager.getGrades()
            .filter { it.subjectId == subjectId }
            .sortedBy { it.date }
            .takeLast(limit)
            .map { 
                GradeDataPoint(
                    date = it.date,
                    score = (it.score / it.maxScore * 100),
                    type = it.type
                )
            }
    }
    
    /**
     * Detect if performance is improving or declining
     */
    fun getPerformanceTrend(subjectId: String): PerformanceTrend {
        val trends = getGradeTrends(subjectId, 5)
        if (trends.size < 2) return PerformanceTrend.STABLE
        
        val recent = trends.takeLast(3).map { it.score }.average()
        val earlier = trends.take(2).map { it.score }.average()
        
        val change = recent - earlier
        
        return when {
            change > 5 -> PerformanceTrend.IMPROVING
            change < -5 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }
    
    /**
     * Compare current performance with historical
     */
    fun compareWithHistorical(subjectId: String): HistoricalComparison? {
        val allGrades = DataManager.getGrades().filter { it.subjectId == subjectId }
        if (allGrades.size < 2) return null
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val oneMonthAgo = calendar.timeInMillis
        
        val recentGrades = allGrades.filter { it.date >= oneMonthAgo }
        val olderGrades = allGrades.filter { it.date < oneMonthAgo }
        
        if (recentGrades.isEmpty() || olderGrades.isEmpty()) return null
        
        val recentAvg = recentGrades.map { it.score / it.maxScore * 100 }.average()
        val olderAvg = olderGrades.map { it.score / it.maxScore * 100 }.average()
        
        return HistoricalComparison(
            currentAverage = recentAvg,
            previousAverage = olderAvg,
            change = recentAvg - olderAvg
        )
    }
    
    /**
     * Get performance summary for all subjects
     */
    fun getAllSubjectPerformance(): List<SubjectPerformance> {
        return DataManager.getSubjects().map { subject ->
            val avgGrade = getAverageGrade(subject.id)
            val trend = getPerformanceTrend(subject.id)
            
            SubjectPerformance(
                subjectId = subject.id,
                averageGrade = avgGrade,
                status = trend
            )
        }
    }
    
    data class GradeDataPoint(
        val date: Long,
        val score: Double,
        val type: String // Renamed from assignmentName to type
    )
    
    enum class PerformanceTrend {
        IMPROVING,
        STABLE,
        DECLINING
    }
    
    data class HistoricalComparison(
        val currentAverage: Double,
        val previousAverage: Double,
        val change: Double
    )
    
    data class SubjectPerformance(
        val subjectId: String,
        val averageGrade: Double?,
        val status: PerformanceTrend // Renamed from trend to status
    )
}

