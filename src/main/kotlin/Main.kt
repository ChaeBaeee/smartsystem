package com.smartstudy

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.smartstudy.data.DataManager
import com.smartstudy.services.AlertService
import com.smartstudy.ui.MainScreen
import java.awt.Dimension

fun main() = application {
    // Load data on startup
    DataManager.loadAllData()
    
    // Check for alerts
    val alertService = AlertService()
    alertService.checkAndGenerateAlerts()
    
    Window(
        onCloseRequest = {
            // Save data on shutdown
            DataManager.saveAllData()
            exitApplication()
        },
        title = "Smart Study & Academic Progress Monitoring System",
        state = WindowState(
            width = 1200.dp,
            height = 800.dp
        )
    ) {
        // Set minimum window size to prevent UI from breaking
        androidx.compose.runtime.LaunchedEffect(Unit) {
            window.minimumSize = Dimension(800, 600)
        }
        MainScreen()
    }
}
