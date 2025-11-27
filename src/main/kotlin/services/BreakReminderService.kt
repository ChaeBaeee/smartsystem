package com.smartstudy.services

import com.smartstudy.models.StudySession
import java.util.Timer
import java.util.TimerTask

class BreakReminderService {
    
    private var reminderTimer: Timer? = null
    private var breakIntervalMinutes: Int = 25 // Default 25 minutes
    private var breakDurationMinutes: Int = 5 // Default 5 minute break
    var onReminderCallback: (() -> Unit)? = null
    
    /**
     * Start monitoring study session for break reminders
     */
    fun startMonitoring(session: StudySession, onReminder: (() -> Unit)? = null) {
        stopMonitoring()
        onReminderCallback = onReminder
        
        reminderTimer = Timer(true)
        reminderTimer?.schedule(object : TimerTask() {
            override fun run() {
                // Callback will be handled by the UI layer
                onReminderCallback?.invoke()
            }
        }, breakIntervalMinutes * 60 * 1000L)
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        reminderTimer?.cancel()
        reminderTimer = null
    }
    
    /**
     * Set break interval
     */
    fun setBreakInterval(minutes: Int) {
        breakIntervalMinutes = minutes.coerceIn(10, 60)
    }
    
    /**
     * Set break duration
     */
    fun setBreakDuration(minutes: Int) {
        breakDurationMinutes = minutes.coerceIn(1, 15)
    }
    
    /**
     * Get recommended break duration based on study time
     */
    fun getRecommendedBreakDuration(studyMinutes: Int): Int {
        return when {
            studyMinutes >= 120 -> 15
            studyMinutes >= 60 -> 10
            studyMinutes >= 30 -> 5
            else -> 3
        }
    }
    
    /**
     * Check if break is needed based on study duration
     */
    fun shouldTakeBreak(session: StudySession): Boolean {
        if (session.endTime == null) {
            val elapsed = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()
            return elapsed >= breakIntervalMinutes
        }
        return false
    }
    
    fun getBreakMessage(): String {
        return "You've been studying for $breakIntervalMinutes minutes. " +
                "Take a $breakDurationMinutes minute break to stay refreshed."
    }
}
