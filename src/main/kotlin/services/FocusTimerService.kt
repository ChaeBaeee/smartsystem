package com.smartstudy.services

import java.util.Timer
import java.util.TimerTask

class FocusTimerService {
    
    private var timer: Timer? = null
    private var remainingSeconds: Int = 0
    private var workIntervalMinutes: Int = 25
    private var breakIntervalMinutes: Int = 5
    private var isWorkPhase: Boolean = true
    private var isRunning: Boolean = false
    
    var onTick: ((Int) -> Unit)? = null
    var onComplete: (() -> Unit)? = null
    
    /**
     * Start focus timer
     */
    fun start(workMinutes: Int = workIntervalMinutes, breakMinutes: Int = breakIntervalMinutes, startWithBreak: Boolean = false) {
        stop()
        workIntervalMinutes = workMinutes
        breakIntervalMinutes = breakMinutes
        isWorkPhase = !startWithBreak
        remainingSeconds = if (startWithBreak) breakMinutes * 60 else workMinutes * 60
        isRunning = true
        
        timer = Timer(true)
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                remainingSeconds--
                onTick?.invoke(remainingSeconds)
                
                if (remainingSeconds <= 0) {
                    if (isWorkPhase) {
                        // Switch to break
                        isWorkPhase = false
                        remainingSeconds = breakIntervalMinutes * 60
                        onComplete?.invoke()
                    } else {
                        // Break complete, stop timer
                        stop()
                        onComplete?.invoke()
                    }
                }
            }
        }, 1000, 1000)
    }
    
    /**
     * Stop timer
     */
    fun stop() {
        timer?.cancel()
        timer = null
        isRunning = false
        remainingSeconds = 0
    }
    
    /**
     * Pause timer
     */
    fun pause() {
        timer?.cancel()
        timer = null
        isRunning = false
    }
    
    /**
     * Resume timer
     */
    fun resume() {
        if (!isRunning && remainingSeconds > 0) {
            isRunning = true
            timer = Timer(true)
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    remainingSeconds--
                    onTick?.invoke(remainingSeconds)
                    
                    if (remainingSeconds <= 0) {
                        if (isWorkPhase) {
                            isWorkPhase = false
                            remainingSeconds = breakIntervalMinutes * 60
                            onComplete?.invoke()
                        } else {
                            stop()
                            onComplete?.invoke()
                        }
                    }
                }
            }, 1000, 1000)
        }
    }
    
    /**
     * Get formatted time string
     */
    fun getFormattedTime(): String {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Check if timer is running
     */
    fun isActive(): Boolean = isRunning
    
    /**
     * Check if in work phase
     */
    fun isWorkPhase(): Boolean = isWorkPhase
    
    /**
     * Get remaining seconds
     */
    fun getRemainingSeconds(): Int = remainingSeconds
}
