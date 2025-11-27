package com.smartstudy.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartstudy.data.DataManager
import com.smartstudy.services.AlertService
import com.smartstudy.services.ProgressTrackingService
import com.smartstudy.services.ReviewSuggestionService
import com.smartstudy.services.StudyScheduleService
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigate: (Screen) -> Unit = {}
    ) {
    val progressService = remember { ProgressTrackingService() }
    val reviewService = remember { ReviewSuggestionService() }
    val scheduleService = remember { StudyScheduleService() }
    val alertService = remember { AlertService() }

    val dataVersion = UiEventBus.dataVersion
    val stats = remember(dataVersion) { progressService.getOverallStatistics() }
    val subjectProgress = remember(dataVersion) { progressService.getAllSubjectProgress() }
    val suggestions = remember(dataVersion) { reviewService.getSuggestedTopics(limit = 5) }
    val nextTopic = suggestions.firstOrNull()
    val upcomingSessions = remember(dataVersion) { getUpcomingSchedulePreview() }
    val alertCount = remember(dataVersion) { alertService.getUnresolvedCount() }
    val streakDays = remember(dataVersion) { calculateStudyStreak() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        // Welcome message - only in Dashboard
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stay focused and keep improving your performance.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Text(
            text = "Today's Overview",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            GradientStatCard(
                modifier = Modifier.weight(1f),
                title = "Total Study Hours",
                value = "${stats.totalStudyMinutes / 60}h",
                subtitle = "All time total",
                gradient = Brush.linearGradient(
                    listOf(Color(0xFF6E8BFF), Color(0xFF8AA9FF))
                )
            )
            GradientStatCard(
                modifier = Modifier.weight(1f),
                title = "Current Streak",
                value = "${streakDays}d",
                subtitle = "Daily focus streak",
                gradient = Brush.linearGradient(
                    listOf(Color(0xFF6FEDD6), Color(0xFF74C69D))
                )
            )
            GradientStatCard(
                modifier = Modifier.weight(1f),
                title = "Open Alerts",
                value = alertCount.toString(),
                subtitle = "New notifications",
                gradient = Brush.linearGradient(
                    listOf(Color(0xFFFFC371), Color(0xFFFF5F6D))
                )
            )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    QuickActionsCard(
                        onStartTimer = { onNavigate(Screen.TIMER) },
                        onViewAnalytics = { onNavigate(Screen.TRACKING) },
                        onGenerateSchedule = {
                            scheduleService.updateScheduleFromPatterns()
                            UiEventBus.notifyDataChanged()
                            scope.launch {
                                snackbarHostState.showSnackbar("Adaptive schedule updated")
                            }
                        },
                        onViewSchedule = { onNavigate(Screen.SCHEDULE) }
                    )
                    UpcomingScheduleCard(upcomingSessions)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TopicReviewCard(
                        nextTopic = nextTopic,
                        suggestions = suggestions,
                        onReview = {
                            nextTopic?.let { reviewService.markAsReviewed(it.topic.id) }
                            UiEventBus.notifyDataChanged()
                            scope.launch {
                                snackbarHostState.showSnackbar("Topic marked as reviewed")
                            }
                        },
                        onNavigate = { onNavigate(Screen.SUBJECTS) }
                    )
                    AlertSummaryCard(
                        alertCount = alertCount,
                        onViewAlerts = { onNavigate(Screen.ALERTS) }
                    )
                }
            }

        SubjectProgressCard(subjectProgress)
        }
        
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GradientStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    gradient: Brush
) {
    Box(
        modifier = modifier
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush = gradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.subtitle2,
                maxLines = 1
            )
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.body2,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    onStartTimer: () -> Unit,
    onViewAnalytics: () -> Unit,
    onGenerateSchedule: () -> Unit,
    onViewSchedule: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    label = "Start Focus Timer",
                    icon = Icons.Filled.PlayArrow,
                    onClick = onStartTimer
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    label = "View Analytics",
                    icon = Icons.Filled.Assessment,
                    onClick = onViewAnalytics
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onGenerateSchedule,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Schedule")
                }
                OutlinedButton(
                    onClick = onViewSchedule,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open Schedule")
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UpcomingScheduleCard(items: List<SchedulePreview>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Upcoming Schedule", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text("No study blocks scheduled. Generate a plan to stay on track.")
            } else {
                items.forEach { session ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(session.dayLabel, fontWeight = FontWeight.Bold)
                            Text(session.time, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(session.subject, fontWeight = FontWeight.Medium)
                            if (session.topic.isNotBlank()) {
                                Text(
                                    session.topic,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun TopicReviewCard(
    nextTopic: ReviewSuggestionService.TopicSuggestion?,
    suggestions: List<ReviewSuggestionService.TopicSuggestion>,
    onReview: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Topic Review", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            nextTopic?.let {
                Text("Next up: ${it.topic.name}", fontWeight = FontWeight.Bold)
                Text(
                    text = "Priority score ${String.format("%.1f", it.priorityScore)} • ${it.daysSinceReview} days since review",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onReview, shape = RoundedCornerShape(16.dp)) {
                        Text("Mark Reviewed")
                    }
                    OutlinedButton(onClick = onNavigate, shape = RoundedCornerShape(16.dp)) {
                        Text("See All Topics")
                    }
                }
            } ?: Text("Add subjects and topics to receive smart review suggestions.")

            Divider()

            suggestions.drop(1).take(3).forEach {
                Text("${it.topic.name} • ${it.daysSinceReview} days ago")
            }
        }
    }
}

@Composable
private fun AlertSummaryCard(
    alertCount: Int,
    onViewAlerts: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color(0xFFFFF6F4),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Performance Alerts",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A) // Dark color for better contrast
            )
            Text(
                text = if (alertCount == 0) {
                    "All caught up! No alerts at the moment."
                } else {
                    "$alertCount areas need your attention."
                },
                color = Color(0xFF4A4A4A) // Dark gray for better contrast
            )
            Button(
                onClick = onViewAlerts,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFF8A65),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Alerts")
            }
        }
    }
}

@Composable
private fun SubjectProgressCard(
    progress: List<ProgressTrackingService.SubjectProgress>
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Subject Progress", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            progress.forEach { item ->
                val subject = DataManager.getSubjects().find { it.id == item.subjectId }
                if (subject != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject.name, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", item.completionPercentage)}%")
                        }
                        LinearProgressIndicator(
                            progress = (item.completionPercentage / 100).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            color = if (item.completionPercentage >= 80) Color(0xFF2ECC71) else Color(0xFF5A6DFF)
                        )
                    }
                }
            }
        }
    }
}

private data class SchedulePreview(
    val dayLabel: String,
    val time: String,
    val subject: String,
    val topic: String
)

private fun getUpcomingSchedulePreview(): List<SchedulePreview> {
    val subjects = DataManager.getSubjects().associateBy { it.id }
    val todayValue = LocalDate.now().dayOfWeek.value // 1 (Mon) - 7 (Sun)
    return DataManager.getScheduleItems()
        .filter { it.enabled }
        .sortedWith(
            compareBy(
                { dayDifference(todayValue, it.dayOfWeek) },
                { it.startTime }
            )
        )
        .take(3)
        .map { item ->
            SchedulePreview(
                dayLabel = dayNameFor(item.dayOfWeek),
                time = item.startTime,
                subject = subjects[item.subjectId]?.name ?: "Unknown",
                topic = item.topic
            )
        }
}

private fun dayDifference(today: Int, target: Int): Int {
    val normalizedTarget = if (target == 0) 7 else target
    var diff = normalizedTarget - today
    if (diff < 0) diff += 7
    return diff
}

private fun dayNameFor(day: Int): String {
    val dow = when (day) {
        0 -> DayOfWeek.SUNDAY
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        else -> DayOfWeek.SATURDAY
    }
    return dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

private fun calculateStudyStreak(): Int {
    val sessions = DataManager.getStudySessions()
        .filter { it.endTime != null }
        .map { startOfDay(it.startTime) }
        .toSet()

    var streak = 0
    var dayPointer = startOfDay(System.currentTimeMillis())
    while (sessions.contains(dayPointer)) {
        streak++
        dayPointer -= DAY_IN_MILLIS
    }
    return streak
}

private fun startOfDay(timestamp: Long): Long {
    val date = LocalDate.ofEpochDay(timestamp / DAY_IN_MILLIS)
    return date.toEpochDay() * DAY_IN_MILLIS
}

private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
