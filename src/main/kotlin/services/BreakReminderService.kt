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
    fun startMonitoring(@Suppress("UNUSED_PARAMETER") session: StudySession, onReminder: (() -> Unit)? = null) {
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
    
    fun getBreakMessage(): String {
        return "You've been studying for $breakIntervalMinutes minutes. " +
                "Take a $breakDurationMinutes minute break to stay refreshed."
    }
}
