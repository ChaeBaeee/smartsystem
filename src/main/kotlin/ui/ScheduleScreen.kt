package com.smartstudy.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartstudy.data.DataManager
import com.smartstudy.services.ReviewSuggestionService
import com.smartstudy.services.StudyScheduleService
import kotlinx.coroutines.launch
import com.smartstudy.ui.components.GradientCard

@Composable
fun ScheduleScreen() {
    val scheduleService = remember { StudyScheduleService() }
    val reviewService = remember { ReviewSuggestionService() }
    val daysOfWeek = remember {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }
    val fullDayNames = remember {
        listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    }

    var refreshTrigger by remember { mutableStateOf(0) }
    val dataVersion = UiEventBus.dataVersion
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<com.smartstudy.models.ScheduleItem?>(null) }

    val nextSession = remember(refreshTrigger, dataVersion) { getNextSessionSummary() }
    val suggestions = remember(refreshTrigger, dataVersion) {
        reviewService.getSuggestedTopics(limit = 3)
    }
    val subjects = remember(dataVersion) { DataManager.getSubjects() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top section - Hero and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScheduleHeroCard(
                    modifier = Modifier.weight(1f),
                    nextSession = nextSession
                )
                Column(
                    modifier = Modifier.weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scheduleService.updateScheduleFromPatterns()
                            UiEventBus.notifyDataChanged()
                            refreshTrigger++
                            scope.launch { snackbarHostState.showSnackbar("Schedule generated!") }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Schedule", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            // Clear all schedule items
                            DataManager.getScheduleItems().forEach { 
                                DataManager.deleteScheduleItem(it.id) 
                            }
                            UiEventBus.notifyDataChanged()
                            refreshTrigger++
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Weekly Calendar - Full Width at the Bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Day headers - clickable tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        daysOfWeek.forEachIndexed { index, dayName ->
                            val isSelected = selectedDay == index
                            val daySchedule = remember(refreshTrigger, dataVersion) { 
                                scheduleService.getScheduleForDay(index) 
                            }
                            val hasItems = daySchedule.isNotEmpty()
                            
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clickable { selectedDay = index },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colors.primary
                                    hasItems -> MaterialTheme.colors.primary.copy(alpha = 0.2f)
                                    else -> Color.Transparent
                                },
                                elevation = if (isSelected) 4.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colors.onSurface,
                                        fontSize = 14.sp
                                    )
                                    if (hasItems) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    if (isSelected) Color.White else MaterialTheme.colors.primary,
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Selected day's schedule
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fullDayNames[selectedDay],
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Session")
                            }
                        }
                        
                        val daySchedule = remember(refreshTrigger, dataVersion, selectedDay) { 
                            scheduleService.getScheduleForDay(selectedDay) 
                        }
                        
                        if (daySchedule.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No sessions scheduled for ${fullDayNames[selectedDay]}",
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Click 'Generate Schedule' to create study sessions",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            daySchedule.forEach { item ->
                                val subject = DataManager.getSubjects().find { it.id == item.subjectId }
                                ScheduleItemCard(
                                    time = item.startTime,
                                    duration = item.durationMinutes,
                                    subjectName = subject?.name ?: "Unknown",
                                    topic = item.topic,
                                    onEdit = { editingItem = item },
                                    onDelete = {
                                        DataManager.deleteScheduleItem(item.id)
                                        UiEventBus.notifyDataChanged()
                                        refreshTrigger++
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Add Session Dialog
    if (showAddDialog) {
        AddScheduleDialog(
            selectedDay = selectedDay,
            fullDayNames = fullDayNames,
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onAdd = { subjectId, startTime, duration, topic ->
                val newItem = com.smartstudy.models.ScheduleItem(
                    id = java.util.UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    dayOfWeek = selectedDay,
                    startTime = startTime,
                    durationMinutes = duration,
                    topic = topic,
                    enabled = true
                )
                DataManager.addScheduleItem(newItem)
                UiEventBus.notifyDataChanged()
                refreshTrigger++
                showAddDialog = false
                scope.launch { snackbarHostState.showSnackbar("Session added!") }
            }
        )
    }
    
    // Edit Session Dialog
    editingItem?.let { item ->
        EditScheduleDialog(
            item = item,
            fullDayNames = fullDayNames,
            subjects = subjects,
            onDismiss = { editingItem = null },
            onSave = { subjectId, startTime, duration, topic ->
                val updatedItem = item.copy(
                    subjectId = subjectId,
                    startTime = startTime,
                    durationMinutes = duration,
                    topic = topic
                )
                DataManager.updateScheduleItem(updatedItem)
                UiEventBus.notifyDataChanged()
                refreshTrigger++
                editingItem = null
                scope.launch { snackbarHostState.showSnackbar("Session updated!") }
            }
        )
    }
}

@Composable
private fun EditScheduleDialog(
    item: com.smartstudy.models.ScheduleItem,
    fullDayNames: List<String>,
    subjects: List<com.smartstudy.models.Subject>,
    onDismiss: () -> Unit,
    onSave: (subjectId: String, startTime: String, duration: Int, topic: String) -> Unit
) {
    val timeParts = item.startTime.split(":")
    var selectedSubjectId by remember { mutableStateOf(item.subjectId) }
    var hour by remember { mutableStateOf(timeParts.getOrNull(0) ?: "09") }
    var minute by remember { mutableStateOf(timeParts.getOrNull(1) ?: "00") }
    var duration by remember { mutableStateOf(item.durationMinutes.toString()) }
    var topic by remember { mutableStateOf(item.topic) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Edit Study Session - ${fullDayNames[item.dayOfWeek]}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subject dropdown
                Text("Subject", fontWeight = FontWeight.Medium)
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedSubjectId = subject.id
                                    expanded = false
                                }
                            ) {
                                Text(subject.name)
                            }
                        }
                    }
                }
                
                // Time input
                Text("Start Time", fontWeight = FontWeight.Medium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hour = it },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minute = it },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Duration
                Text("Duration (minutes)", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duration = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Topic
                Text("Topic (optional)", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Chapter 5 Review") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 9
                    val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val d = duration.toIntOrNull()?.coerceIn(1, 480) ?: 60
                    val timeStr = String.format("%02d:%02d", h, m)
                    if (selectedSubjectId.isNotEmpty()) {
                        onSave(selectedSubjectId, timeStr, d, topic)
                    }
                },
                enabled = selectedSubjectId.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun AddScheduleDialog(
    selectedDay: Int,
    fullDayNames: List<String>,
    subjects: List<com.smartstudy.models.Subject>,
    onDismiss: () -> Unit,
    onAdd: (subjectId: String, startTime: String, duration: Int, topic: String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
    var hour by remember { mutableStateOf("09") }
    var minute by remember { mutableStateOf("00") }
    var duration by remember { mutableStateOf("60") }
    var topic by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Add Study Session - ${fullDayNames[selectedDay]}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subject dropdown
                Text("Subject", fontWeight = FontWeight.Medium)
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedSubjectId = subject.id
                                    expanded = false
                                }
                            ) {
                                Text(subject.name)
                            }
                        }
                    }
                }
                
                // Time input
                Text("Start Time", fontWeight = FontWeight.Medium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hour = it },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minute = it },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Duration
                Text("Duration (minutes)", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duration = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Topic
                Text("Topic (optional)", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Chapter 5 Review") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 9
                    val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val d = duration.toIntOrNull()?.coerceIn(1, 480) ?: 60
                    val timeStr = String.format("%02d:%02d", h, m)
                    if (selectedSubjectId.isNotEmpty()) {
                        onAdd(selectedSubjectId, timeStr, d, topic)
                    }
                },
                enabled = selectedSubjectId.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ScheduleItemCard(
    time: String,
    duration: Int,
    subjectName: String,
    topic: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colors.primary.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = time,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.primary
                )
                Text(
                    text = "$duration min",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Subject and topic
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (topic.isNotEmpty()) {
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Edit button
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colors.primary
                )
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE74C3C)
                )
            }
        }
    }
}

@Composable
private fun ScheduleHeroCard(
    modifier: Modifier = Modifier,
    nextSession: NextSessionSummary?
) {
    GradientCard(
        modifier = modifier,
        colors = listOf(Color(0xFF7F7CFF), Color(0xFFA6B7FF))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Adaptive Schedule", color = Color.White.copy(alpha = 0.9f))
            if (nextSession != null) {
                Text(
                    nextSession.subject,
                    color = Color.White,
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${nextSession.day} · ${nextSession.time} · ${nextSession.duration} min",
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (nextSession.topic.isNotEmpty()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = nextSession.topic,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "No sessions scheduled. Generate an adaptive plan to stay on track.",
                    color = Color.White,
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}


private data class NextSessionSummary(
    val subject: String,
    val day: String,
    val time: String,
    val duration: Int,
    val topic: String
)

private fun getNextSessionSummary(): NextSessionSummary? {
    val items = DataManager.getScheduleItems().filter { it.enabled }
    val subjects = DataManager.getSubjects().associateBy { it.id }
    val next = items.minByOrNull { it.dayOfWeek * 24 * 60 + parseTime(it.startTime) }
    return next?.let {
        NextSessionSummary(
            subject = subjects[it.subjectId]?.name ?: "Subject",
            day = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").getOrElse(it.dayOfWeek) { "Day" },
            time = it.startTime,
            duration = it.durationMinutes,
            topic = it.topic
        )
    }
}

private fun parseTime(time: String): Int {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour * 60 + minute
}
