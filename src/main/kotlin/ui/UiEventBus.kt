package com.smartstudy.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Simple global notifier so different screens can react when underlying data changes.
 */
object UiEventBus {
    var dataVersion by mutableStateOf(0)
        private set

    fun notifyDataChanged() {
        dataVersion++
    }
}

