package com.smartstudy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartstudy.data.DataManager
import com.smartstudy.services.BreakReminderService
import com.smartstudy.services.FocusTimerService
import com.smartstudy.services.TimeTrackingService
import com.smartstudy.utils.hexToColor
import kotlinx.coroutines.delay

private enum class TimerTab { Study, Break }

@Composable
fun StudyTimerScreen() {
    val timerService = remember { FocusTimerService() }
    val breakReminderService = remember { BreakReminderService() }
    val timeTrackingService = remember { TimeTrackingService() }
    val dataVersion = UiEventBus.dataVersion

    var workMinutes by remember { mutableStateOf("25") }
    var breakMinutes by remember { mutableStateOf("5") }
    var selectedTab by remember { mutableStateOf(TimerTab.Study) }
    var isRunning by remember { mutableStateOf(false) }
    var activeSessionId by remember { mutableStateOf<String?>(null) }
    var sessionStatus by remember { mutableStateOf("No active session") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var topicText by remember { mutableStateOf("") }
    var showBreakDialog by remember { mutableStateOf(false) }

    // Break reminder callback
    breakReminderService.onReminderCallback = {
        showBreakDialog = true
    }

    val workDurationSeconds = remember(workMinutes) { 
        (workMinutes.toIntOrNull()?.coerceIn(1, 999)?.times(60)) ?: (25 * 60)
    }
    val breakDurationSeconds = remember(breakMinutes) { 
        (breakMinutes.toIntOrNull()?.coerceIn(1, 999)?.times(60)) ?: (5 * 60)
    }
    val currentWorkDuration = workDurationSeconds
    val currentBreakDuration = breakDurationSeconds

    var timerServiceSeconds by remember { mutableStateOf(0) }
    var timerServiceIsWork by remember { mutableStateOf(true) }
    
    // Calculate display values directly from reactive inputs - updates instantly as user types
    // Don't use remember here so it recomputes on every composition when inputs change
    val totalSeconds = if (isRunning) {
        // When running, use timer service phase
        if (timerServiceIsWork) currentWorkDuration else currentBreakDuration
    } else {
        // When not running, use selected tab (updates instantly as user types)
        if (selectedTab == TimerTab.Study) currentWorkDuration else currentBreakDuration
    }
    
    val remainingSeconds = if (isRunning) {
        // When running, use timer service value
        if (timerServiceSeconds > 0) timerServiceSeconds else totalSeconds
    } else {
        // When not running
        if (timerServiceSeconds > 0) {
            // Timer is paused - preserve the remaining seconds from service
            timerServiceSeconds
        } else {
            // Timer is stopped - use target duration (updates instantly as user types)
            totalSeconds
        }
    }
    
    // Timer state updater - updates state from service
    LaunchedEffect(Unit) {
        while (true) {
            val running = timerService.isActive()
            val isWork = timerService.isWorkPhase()
            val serviceSeconds = timerService.getRemainingSeconds()
            
            // Update selectedTab based on timer phase when running
            if (running) {
                selectedTab = if (isWork) TimerTab.Study else TimerTab.Break
            }
            
            isRunning = running
            timerServiceSeconds = serviceSeconds
            timerServiceIsWork = isWork
            // Values are used in the UI through reactive state
            delay(200)
        }
    }

    // Session status update
    LaunchedEffect(Unit) {
        while (true) {
            val session = timeTrackingService.getActiveSession()
            sessionStatus = if (session != null) {
                val elapsed = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()
                "Active session · $elapsed min"
            } else {
                "No active session"
            }
            delay(1000)
        }
    }

    val subjects = remember(dataVersion) { DataManager.getSubjects() }
    LaunchedEffect(subjects) {
        if (selectedSubject == null && subjects.isNotEmpty()) {
            selectedSubject = subjects.first().name
        }
        if (selectedSubject != null && subjects.none { it.name == selectedSubject }) {
            selectedSubject = subjects.firstOrNull()?.name
        }
    }

    val elapsedMinutes = ((totalSeconds - remainingSeconds).coerceAtLeast(0)) / 60
    val timerPhaseLabel = if (selectedTab == TimerTab.Study) "Study" else "Break"
    val timerText = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val secs = remainingSeconds % 60
        String.format("%02d:%02d", minutes, secs)
    }
    // Calculate progress: 0 when timer starts (remainingSeconds == totalSeconds), 1 when complete (remainingSeconds == 0)
    val progress = if (totalSeconds == 0 || remainingSeconds >= totalSeconds) {
        0f
    } else {
        1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
    }

    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TimerCard(
                modifier = Modifier.weight(1.1f),
                selectedTab = selectedTab,
                onTabSelected = { 
                    if (!isRunning) {
                        selectedTab = it
                        // Stop the timer service to reset it (display will update automatically via computed values)
                        timerService.stop()
                    }
                },
                isRunning = isRunning,
                timerPhaseLabel = timerPhaseLabel,
                timeText = timerText,
                progress = progress,
                workMinutes = workMinutes,
                breakMinutes = breakMinutes,
                onWorkMinutesChanged = { workMinutes = it },
                onBreakMinutesChanged = { breakMinutes = it },
                onStart = {
                    val work = workMinutes.toIntOrNull()?.coerceIn(1, 999) ?: 25
                    val breakLen = breakMinutes.toIntOrNull()?.coerceIn(1, 999) ?: 5
                    val startWithBreak = selectedTab == TimerTab.Break
                    timerService.start(work, breakLen, startWithBreak = startWithBreak)
                    // Don't change selectedTab - preserve user's choice
                },
                onResume = { 
                    // Check if selected tab matches the timer service's current phase
                    val timerIsWorkPhase = timerService.isWorkPhase()
                    val selectedIsStudy = selectedTab == TimerTab.Study
                    
                    // If tabs don't match, reset to selected tab's duration instead of resuming
                    if (timerIsWorkPhase != selectedIsStudy) {
                        val work = workMinutes.toIntOrNull()?.coerceIn(1, 999) ?: 25
                        val breakLen = breakMinutes.toIntOrNull()?.coerceIn(1, 999) ?: 5
                        val startWithBreak = selectedTab == TimerTab.Break
                        timerService.start(work, breakLen, startWithBreak = startWithBreak)
                    } else {
                        // Tabs match, so resume normally
                        timerService.resume()
                    }
                },
                onPause = { timerService.pause() },
                onReset = {
                    timerService.stop()
                    selectedTab = TimerTab.Study
                    workMinutes = "25"
                    breakMinutes = "5"
                    isRunning = false
                },
                canResume = !isRunning && timerService.getRemainingSeconds() > 0
            )

            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SessionTrackingCard(
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    onSubjectSelected = { selectedSubject = it },
                    topicText = topicText,
                    onTopicChange = { topicText = it },
                    elapsedMinutes = elapsedMinutes,
                    sessionStatus = sessionStatus,
                    onStartSession = {
                        val subject = subjects.find { it.name == selectedSubject }
                        if (subject != null) {
                            val session = timeTrackingService.startSession(subject.id, topicText.trim())
                            activeSessionId = session.id
                            breakReminderService.startMonitoring(session)
                            // Also start the study timer
                            val work = workMinutes.toIntOrNull()?.coerceIn(1, 999) ?: 25
                            val breakLen = breakMinutes.toIntOrNull()?.coerceIn(1, 999) ?: 5
                            selectedTab = TimerTab.Study
                            timerService.start(work, breakLen, startWithBreak = false)
                            UiEventBus.notifyDataChanged()
                        }
                    },
                    onEndSession = {
                        timerService.stop()
                        activeSessionId?.let {
                            timeTrackingService.endSession(it)
                            breakReminderService.stopMonitoring()
                            UiEventBus.notifyDataChanged()
                        }
                        activeSessionId = null
                    }
                )

                FocusSummaryCard(
                    phase = timerPhaseLabel,
                    elapsedMinutes = elapsedMinutes,
                    totalMinutes = totalSeconds / 60,
                    isRunning = isRunning
                )
            }
        }
        }
        
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }

    if (showBreakDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Break Reminder", fontWeight = FontWeight.Bold) },
            text = { Text(breakReminderService.getBreakMessage()) },
            confirmButton = {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF27AE60))
                ) {
                    Text("Take Break")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TimerCard(
    modifier: Modifier,
    selectedTab: TimerTab,
    onTabSelected: (TimerTab) -> Unit,
    isRunning: Boolean,
    timerPhaseLabel: String,
    timeText: String,
    progress: Float,
    workMinutes: String,
    breakMinutes: String,
    onWorkMinutesChanged: (String) -> Unit,
    onBreakMinutesChanged: (String) -> Unit,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    canResume: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        elevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF5A6DFF), Color(0xFF7C89FF), Color(0xFF95A4FF))
                    )
                )
                .padding(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TimerModeSelector(selectedTab = selectedTab, isLocked = isRunning, onSelect = onTabSelected)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularTimerDial(
                        progress = progress.coerceIn(0f, 1f),
                        timeText = timeText,
                        phaseLabel = timerPhaseLabel
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isRunning) "Currently running" else "Ready to focus",
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isRunning) "Keep going! We'll switch phases automatically." else "Customize durations and tap Start.",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimerStatChip(title = "Study", value = "$workMinutes min")
                            TimerStatChip(title = "Break", value = "$breakMinutes min")
                        }
                    }
                }

                TimerControlsRow(
                    isRunning = isRunning,
                    canResume = canResume,
                    onStart = onStart,
                    onResume = onResume,
                    onPause = onPause,
                    onReset = onReset
                )

                DurationInputs(
                    workMinutes = workMinutes,
                    breakMinutes = breakMinutes,
                    onWorkMinutesChanged = onWorkMinutesChanged,
                    onBreakMinutesChanged = onBreakMinutesChanged,
                    isLocked = isRunning
                )
            }
        }
    }
}

@Composable
private fun TimerModeSelector(
    selectedTab: TimerTab,
    isLocked: Boolean,
    onSelect: (TimerTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimerModeChip(
            label = "Study",
            selected = selectedTab == TimerTab.Study,
            onClick = { if (!isLocked) onSelect(TimerTab.Study) },
            modifier = Modifier.weight(1f)
        )
        TimerModeChip(
            label = "Break",
            selected = selectedTab == TimerTab.Break,
            onClick = { if (!isLocked) onSelect(TimerTab.Break) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimerModeChip(
    label: String, 
    selected: Boolean, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                enabled = !selected,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White else Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (selected) Color.White else Color.Transparent)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF5A6DFF) else Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun CircularTimerDial(
    progress: Float,
    timeText: String,
    phaseLabel: String
) {
    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            val strokeWidth = size.minDimension * 0.08f
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF8DEFFF),
                        Color(0xFF5B6BFF),
                        Color(0xFF8DEFFF)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(phaseLabel, color = Color.White.copy(alpha = 0.8f))
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun TimerControlsRow(
    isRunning: Boolean,
    canResume: Boolean,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val primaryLabel = when {
            canResume -> "Resume"
            isRunning -> "Restart"
            else -> "Start"
        }
        val primaryAction = when {
            canResume -> onResume
            else -> onStart
        }
        Button(
            onClick = primaryAction,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color(0xFF5A6DFF)
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(primaryLabel)
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = isRunning) { onPause() },
            shape = RoundedCornerShape(16.dp),
            color = if (isRunning) Color.White.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PauseCircle, contentDescription = null, tint = if (isRunning) Color.White else Color.White.copy(alpha = 0.3f))
                Spacer(Modifier.width(4.dp))
                Text("Pause", color = if (isRunning) Color.White else Color.White.copy(alpha = 0.3f))
            }
        }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.StopCircle, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Reset")
        }
    }
}

@Composable
fun DurationInputs(
    workMinutes: String,
    breakMinutes: String,
    onWorkMinutesChanged: (String) -> Unit,
    onBreakMinutesChanged: (String) -> Unit,
    isLocked: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Customize durations", color = Color.White.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DurationField(
                label = "Study (min)",
                value = workMinutes,
                onValueChange = onWorkMinutesChanged,
                enabled = !isLocked,
                modifier = Modifier.weight(1f)
            )
            DurationField(
                label = "Break (min)",
                value = breakMinutes,
                onValueChange = onBreakMinutesChanged,
                enabled = !isLocked,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DurationField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 3) onValueChange(it.filter { ch -> ch.isDigit() }) },
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
            disabledTextColor = Color.White.copy(alpha = 0.5f),
            cursorColor = Color.White
        )
    )
}

@Composable
fun TimerStatChip(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SessionTrackingCard(
    subjects: List<com.smartstudy.models.Subject>,
    selectedSubject: String?,
    onSubjectSelected: (String?) -> Unit,
    topicText: String,
    onTopicChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") elapsedMinutes: Int,
    sessionStatus: String,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Study Session Logger", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(selectedSubject ?: "Select Subject", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    subjects.forEach { subject ->
                        val subjectColor = hexToColor(subject.color)
                        DropdownMenuItem(
                            onClick = {
                                onSubjectSelected(subject.name)
                                expanded = false
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(color = subjectColor, shape = CircleShape)
                                )
                                Text(subject.name)
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = topicText,
                onValueChange = onTopicChange,
                label = { Text("Topic (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF27AE60))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Start Tracking",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onEndSession,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.StopCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("End & Save")
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp
            ) {
                Text(
                    text = sessionStatus,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FocusSummaryCard(
    phase: String,
    elapsedMinutes: Int,
    totalMinutes: Int,
    isRunning: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Focus Summary", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryTile(title = "Phase", value = phase)
                SummaryTile(title = "Elapsed", value = "${elapsedMinutes}m")
                SummaryTile(title = "Target", value = "${totalMinutes}m")
            }
            Divider()
            Text(
                text = if (isRunning) "Timer is running. We'll remind you when it's break time." else "Press start to begin the next focus block.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SummaryTile(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DropdownMenuButton(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(selectedItem ?: label, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                ) {
                    Text(item)
                }
            }
        }
    }
}

