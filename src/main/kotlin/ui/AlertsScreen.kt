package com.smartstudy.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartstudy.data.DataManager
import com.smartstudy.services.AlertService
import com.smartstudy.ui.UiEventBus
import com.smartstudy.utils.hexToColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen() {
    val alertService = remember { AlertService() }
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy HH:mm") }

    var refreshTrigger by remember { mutableStateOf(0) }
    val unresolvedCount = remember(refreshTrigger) { alertService.getUnresolvedCount() }
    val highPriorityAlerts = remember(refreshTrigger) { alertService.getAlertsBySeverity(3) }
    val mediumPriorityAlerts = remember(refreshTrigger) { alertService.getAlertsBySeverity(2) }
    val allAlerts = remember(refreshTrigger) { alertService.getUnresolvedAlerts() }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Performance Alerts", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                Text("Scan and resolve issues before they impact performance",
                    style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        alertService.checkAndGenerateAlerts()
                        refreshTrigger++
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Now", style = MaterialTheme.typography.body1)
                }
                Button(
                    onClick = {
                        // Reload alerts from DataManager and refresh UI
                        alertService.checkAndGenerateAlerts()
                        refreshTrigger++
                        UiEventBus.notifyDataChanged()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh", style = MaterialTheme.typography.body1)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AlertStatCard(
                modifier = Modifier.weight(1f),
                title = "Unresolved",
                value = unresolvedCount.toString(),
                description = "Needs attention",
                color = Color(0xFFE57373)
            )
            AlertStatCard(
                modifier = Modifier.weight(1f),
                title = "High Priority",
                value = highPriorityAlerts.size.toString(),
                description = "Critical risks",
                color = Color(0xFFF06292)
            )
            AlertStatCard(
                modifier = Modifier.weight(1f),
                title = "Medium Priority",
                value = mediumPriorityAlerts.size.toString(),
                description = "Monitor soon",
                color = Color(0xFFFFB74D)
            )
        }

        AlertListSection(
            title = "High Priority",
            alerts = highPriorityAlerts,
            tint = Color(0xFFE74C3C),
            dateFormat = dateFormat
        ) {
            alertService.resolveAlert(it)
            refreshTrigger++
            UiEventBus.notifyDataChanged()
        }

        AlertListSection(
            title = "Medium Priority",
            alerts = mediumPriorityAlerts,
            tint = Color(0xFFF39C12),
            dateFormat = dateFormat
        ) {
            alertService.resolveAlert(it)
            refreshTrigger++
            UiEventBus.notifyDataChanged()
        }

        AlertListSection(
            title = "All Alerts",
            alerts = allAlerts,
            tint = MaterialTheme.colors.primary,
            dateFormat = dateFormat
        ) {
            alertService.resolveAlert(it)
            refreshTrigger++
            UiEventBus.notifyDataChanged()
        }
        }
        
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
private fun AlertStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    description: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = color.copy(alpha = 0.12f),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.caption.copy(color = color.copy(alpha = 0.8f)))
            Text(value, style = MaterialTheme.typography.h5.copy(color = color), fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)))
        }
    }
}

@Composable
private fun AlertListSection(
    title: String,
    alerts: List<com.smartstudy.models.PerformanceAlert>,
    tint: Color,
    dateFormat: SimpleDateFormat,
    onResolve: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 500.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.h6.copy(color = tint), fontWeight = FontWeight.Bold)
            if (alerts.isEmpty()) {
                Text("No alerts in this category.", style = MaterialTheme.typography.body2)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts) { alert ->
                        AlertItem(alert, dateFormat, onResolve = { onResolve(alert.id) }, tint = tint)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertItem(
    alert: com.smartstudy.models.PerformanceAlert,
    dateFormat: SimpleDateFormat,
    onResolve: () -> Unit,
    tint: Color
) {
    val subjectObj = alert.subjectId?.let { id ->
        DataManager.getSubjects().find { s -> s.id == id }
    }
    val subject = subjectObj?.name ?: "General"
    val subjectColor = subjectObj?.let { hexToColor(it.color) } ?: Color(0xFF3498DB)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = subjectColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(2.dp, subjectColor.copy(alpha = 0.3f)),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(subjectColor, CircleShape)
                )
                Text(subject, style = MaterialTheme.typography.body1.copy(color = subjectColor), fontWeight = FontWeight.Bold)
            }
            Text(alert.message, style = MaterialTheme.typography.body2)
            Text(dateFormat.format(Date(alert.date)), style = MaterialTheme.typography.caption)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Severity ${alert.severity}", style = MaterialTheme.typography.body2.copy(color = tint), fontWeight = FontWeight.SemiBold)
                if (!alert.resolved) {
                    OutlinedButton(onClick = onResolve, shape = RoundedCornerShape(12.dp)) {
                        Text("Resolve", style = MaterialTheme.typography.body1)
                    }
                }
            }
        }
    }
}

