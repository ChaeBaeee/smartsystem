package com.smartstudy.services

import com.smartstudy.data.DataManager
import com.smartstudy.models.StudySession
import java.util.*

class TimeTrackingService {
    
    private var activeSession: StudySession? = null
    
    /**
     * Start a new study session
     */
    fun startSession(subjectId: String, topic: String = ""): StudySession {
        // End any existing active session
        activeSession?.let { endSession(it.id) }
        
        val session = StudySession(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            topic = topic,
            startTime = System.currentTimeMillis(),
            endTime = null,
            durationMinutes = 0
        )
        
        activeSession = session
        DataManager.addStudySession(session)
        return session
    }
    
    /**
     * End an active study session
     */
    fun endSession(sessionId: String? = null): StudySession? {
        val session = sessionId?.let { 
            DataManager.getStudySessions().find { it.id == sessionId }
        } ?: activeSession
        
        if (session != null && session.endTime == null) {
            val duration = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()
            val updated = session.copy(
                endTime = System.currentTimeMillis(),
                durationMinutes = duration
            )
            DataManager.updateStudySession(updated)
            activeSession = null
            return updated
        }
        
        return null
    }
    
    /**
     * Get active session if any
     */
    fun getActiveSession(): StudySession? = activeSession
    
    /**
     * Add manual study session entry
     */
    fun addManualSession(
        subjectId: String,
        startTime: Long,
        durationMinutes: Int,
        topic: String = "",
        notes: String = ""
    ): StudySession {
        val session = StudySession(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            topic = topic,
            startTime = startTime,
            endTime = startTime + (durationMinutes * 60000L),
            durationMinutes = durationMinutes,
            notes = notes
        )
        
        DataManager.addStudySession(session)
        return session
    }
    
    /**
     * Get total study time for a subject
     */
    fun getTotalStudyTime(subjectId: String, startDate: Long? = null, endDate: Long? = null): Int {
        return DataManager.getStudySessions()
            .filter { it.subjectId == subjectId && it.endTime != null }
            .filter { 
                val start = startDate ?: Long.MIN_VALUE
                val end = endDate ?: Long.MAX_VALUE
                it.startTime >= start && it.startTime <= end
            }
            .sumOf { it.durationMinutes }
    }
    
    /**
     * Get study sessions filtered by criteria
     */
    fun getSessions(
        subjectId: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): List<StudySession> {
        return DataManager.getStudySessions()
            .filter { subjectId == null || it.subjectId == subjectId }
            .filter {
                val start = startDate ?: Long.MIN_VALUE
                val end = endDate ?: Long.MAX_VALUE
                it.startTime >= start && it.startTime <= end
            }
            .sortedByDescending { it.startTime }
    }
    
    /**
     * Get study statistics
     */
    fun getStatistics(subjectId: String? = null): StudyStatistics {
        val sessions = getSessions(subjectId)
            .filter { it.endTime != null }
        
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val averageSessionLength = if (sessions.isNotEmpty()) {
            totalMinutes / sessions.size
        } else 0
        
        val sessionsByDay = sessions.groupBy {
            java.util.Calendar.getInstance().apply {
                timeInMillis = it.startTime
            }.get(java.util.Calendar.DAY_OF_WEEK)
        }
        
        val mostActiveDay = sessionsByDay.maxByOrNull { it.value.size }?.key
        
        return StudyStatistics(
            totalMinutes = totalMinutes,
            totalSessions = sessions.size,
            averageSessionLength = averageSessionLength,
            mostActiveDay = mostActiveDay
        )
    }
    
    data class StudyStatistics(
        val totalMinutes: Int,
        val totalSessions: Int,
        val averageSessionLength: Int,
        val mostActiveDay: Int?
    )
}

