package com.smartstudy.services

import com.smartstudy.data.DataManager
import com.smartstudy.models.AlertType
import com.smartstudy.models.PerformanceAlert
import java.util.*

class AlertService {
    
    private val performanceMonitoringService = PerformanceMonitoringService()
    private val progressTrackingService = ProgressTrackingService()
    
    /**
     * Check for performance issues and generate alerts
     */
    fun checkAndGenerateAlerts() {
        checkGradeDrops()
        checkMissedStudyGoals()
        checkPerformanceDecline()
    }
    
    /**
     * Check for significant grade drops (>10%)
     */
    private fun checkGradeDrops() {
        DataManager.getSubjects().forEach { subject ->
            val trends = performanceMonitoringService.getGradeTrends(subject.id, 3)
            if (trends.size >= 2) {
                val recent = trends.last().score
                val previous = trends[trends.size - 2].score
                val drop = previous - recent
                
                if (drop > 10) {
                    val existingAlert = DataManager.getAlerts()
                        .find { 
                            it.type == AlertType.GRADE_DROP && 
                            it.subjectId == subject.id && 
                            !it.resolved 
                        }
                    
                    if (existingAlert == null) {
                        val alert = PerformanceAlert(
                            id = UUID.randomUUID().toString(),
                            type = AlertType.GRADE_DROP,
                            subjectId = subject.id,
                            message = "Grade dropped by ${String.format("%.1f", drop)}% in ${subject.name}. " +
                                    "Consider reviewing recent topics and seeking help.",
                            date = System.currentTimeMillis(),
                            resolved = false,
                            severity = if (drop > 20) 3 else 2
                        )
                        DataManager.addAlert(alert)
                    }
                }
            }
        }
    }
    
    /**
     * Check for missed study goals
     */
    private fun checkMissedStudyGoals() {
        val allProgress = progressTrackingService.getAllSubjectProgress()
        
        allProgress.forEach { progress ->
            if (progress.completionPercentage < 50) {
                val subject = DataManager.getSubjects().find { it.id == progress.subjectId }
                if (subject != null) {
                    val existingAlert = DataManager.getAlerts()
                        .find { 
                            it.type == AlertType.MISSED_STUDY_GOAL && 
                            it.subjectId == subject.id && 
                            !it.resolved 
                        }
                    
                    if (existingAlert == null) {
                        val alert = PerformanceAlert(
                            id = UUID.randomUUID().toString(),
                            type = AlertType.MISSED_STUDY_GOAL,
                            subjectId = subject.id,
                            message = "Study goal completion is ${String.format("%.1f", progress.completionPercentage)}% " +
                                    "for ${subject.name}. Consider adjusting your schedule.",
                            date = System.currentTimeMillis(),
                            resolved = false,
                            severity = 2
                        )
                        DataManager.addAlert(alert)
                    }
                }
            }
        }
    }
    
    /**
     * Check for general performance decline
     */
    private fun checkPerformanceDecline() {
        DataManager.getSubjects().forEach { subject ->
            val trend = performanceMonitoringService.getPerformanceTrend(subject.id)
            
            if (trend == PerformanceMonitoringService.PerformanceTrend.DECLINING) {
                val existingAlert = DataManager.getAlerts()
                    .find { 
                        it.type == AlertType.PERFORMANCE_DECLINE && 
                        it.subjectId == subject.id && 
                        !it.resolved 
                    }
                
                if (existingAlert == null) {
                    val alert = PerformanceAlert(
                        id = UUID.randomUUID().toString(),
                        type = AlertType.PERFORMANCE_DECLINE,
                        subjectId = subject.id,
                        message = "Performance trend is declining in ${subject.name}. " +
                                "Review recent topics and consider additional study time.",
                        date = System.currentTimeMillis(),
                        resolved = false,
                        severity = 2
                    )
                    DataManager.addAlert(alert)
                }
            }
        }
    }
    
    /**
     * Get unresolved alerts
     */
    fun getUnresolvedAlerts(): List<PerformanceAlert> {
        return DataManager.getAlerts()
            .filter { !it.resolved }
            .sortedByDescending { it.date }
    }
    
    /**
     * Get alerts by severity
     */
    fun getAlertsBySeverity(severity: Int): List<PerformanceAlert> {
        return DataManager.getAlerts()
            .filter { it.severity == severity && !it.resolved }
            .sortedByDescending { it.date }
    }
    
    /**
     * Resolve an alert
     */
    fun resolveAlert(alertId: String) {
        val alert = DataManager.getAlerts().find { it.id == alertId }
        if (alert != null && !alert.resolved) {
            val updated = alert.copy(resolved = true)
            DataManager.updateAlert(updated)
            // Ensure data is saved immediately
            DataManager.saveAllData()
        }
    }
    
    /**
     * Get alert count
     */
    fun getUnresolvedCount(): Int {
        return DataManager.getAlerts().count { !it.resolved }
    }
}

