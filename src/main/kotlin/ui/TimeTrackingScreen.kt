package com.smartstudy.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartstudy.data.DataManager
import com.smartstudy.services.TimeTrackingService
import com.smartstudy.utils.hexToColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimeTrackingScreen() {
    val timeTrackingService = remember { TimeTrackingService() }
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy HH:mm") }
    val dataVersion = UiEventBus.dataVersion
    
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf(timeTrackingService.getSessions()) }
    val stats = remember(dataVersion) { timeTrackingService.getStatistics() }
    val subjectsSnapshot = remember(dataVersion) { DataManager.getSubjects() }
    
    LaunchedEffect(dataVersion) {
        sessions = timeTrackingService.getSessions()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Study Time Tracking",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        
        // Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Total Study Time",
                value = "${stats.totalMinutes / 60}h ${stats.totalMinutes % 60}m",
                icon = null,
                color = Color(0xFF3498DB),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Sessions",
                value = "${stats.totalSessions}",
                icon = null,
                color = Color(0xFF9B59B6),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Avg Session Length",
                value = "${stats.averageSessionLength} min",
                icon = null,
                color = Color(0xFF27AE60),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Filter
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colors.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subjects = listOf("All Subjects") + subjectsSnapshot.map { it.name }
                DropdownMenuButton(
                    items = subjects,
                    selectedItem = selectedSubject ?: "All Subjects",
                    onItemSelected = {
                        selectedSubject = if (it == "All Subjects") null else it
                        sessions = if (selectedSubject == null) {
                            timeTrackingService.getSessions()
                        } else {
                            val subject = subjectsSnapshot.find { s -> s.name == selectedSubject }
                            if (subject != null) {
                                timeTrackingService.getSessions(subject.id)
                            } else {
                                emptyList()
                            }
                        }
                    },
                    label = "Filter by Subject"
                )
                OutlinedButton(
                    onClick = {
                        sessions = if (selectedSubject == null) {
                            timeTrackingService.getSessions()
                        } else {
                            val subject = subjectsSnapshot.find { s -> s.name == selectedSubject }
                            if (subject != null) {
                                timeTrackingService.getSessions(subject.id)
                            } else {
                                emptyList()
                            }
                        }
                    }
                ) {
                    Text("Filter", style = MaterialTheme.typography.body1)
                }
                OutlinedButton(
                    onClick = {
                        sessions = timeTrackingService.getSessions()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh", style = MaterialTheme.typography.body1)
                }
            }
        }
        
        // Sessions List
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colors.surface
        ) {
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No study sessions recorded",
                            style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { session ->
                        val subject = DataManager.getSubjects().find { it.id == session.subjectId }
                        val subjectColor: Color = subject?.let { hexToColor(it.color) } ?: Color(0xFF3498DB)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = subjectColor.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, subjectColor.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(color = subjectColor, shape = CircleShape)
                                        )
                                        Text(
                                            text = subject?.name ?: "Unknown",
                                            style = MaterialTheme.typography.subtitle1.copy(color = subjectColor),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = subjectColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${session.durationMinutes} min",
                                                style = MaterialTheme.typography.body2.copy(color = subjectColor),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                DataManager.deleteStudySession(session.id)
                                                sessions = timeTrackingService.getSessions()
                                                UiEventBus.notifyDataChanged()
                                            }
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFE74C3C))
                                        }
                                    }
                                }
                                
                                if (session.topic.isNotEmpty()) {
                                    Text(
                                        text = "Topic: ${session.topic}",
                                        style = MaterialTheme.typography.body2,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Text(
                                    text = dateFormat.format(Date(session.startTime)),
                                    style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                )
                                
                                if (session.notes.isNotEmpty()) {
                                    Text(
                                        text = session.notes,
                                        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    }
                    
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(listState)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onSurface),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
