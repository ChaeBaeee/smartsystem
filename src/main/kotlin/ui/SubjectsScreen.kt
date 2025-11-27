package com.smartstudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.Icons.Default
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import com.smartstudy.data.DataManager
import com.smartstudy.models.Subject
import com.smartstudy.models.Topic
import com.smartstudy.services.ReviewSuggestionService
import com.smartstudy.ui.UiEventBus
import java.util.UUID

// Helper function to parse hex color string to Color
private fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorValue = when (cleanHex.length) {
            6 -> cleanHex.toLong(16) or 0xFF000000
            8 -> cleanHex.toLong(16)
            else -> 0xFF3498DB // Default blue
        }
        Color(colorValue.toULong())
    } catch (e: Exception) {
        Color(0xFF3498DB) // Default blue on error
    }
}

// Helper function to lighten a color for better visibility
private fun Color.lighten(factor: Float = 0.3f): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

@Composable
fun SubjectsScreen(
    onNavigateToTimer: () -> Unit = {}
) {
    val reviewService = remember { ReviewSuggestionService() }
    val dataVersion = UiEventBus.dataVersion

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var topicSubjectId by remember { mutableStateOf<String?>(null) }
    var editingTopic by remember { mutableStateOf<Topic?>(null) }

    var subjects by remember { mutableStateOf(DataManager.getSubjects()) }
    var suggestions by remember { mutableStateOf(reviewService.getSuggestedTopics(limit = 8)) }
    var topics by remember { mutableStateOf<List<Topic>>(emptyList()) }

    LaunchedEffect(selectedSubjectId, dataVersion) {
        subjects = DataManager.getSubjects()
        if (selectedSubjectId == null && subjects.isNotEmpty()) {
            selectedSubjectId = subjects.first().id
        }
        suggestions = reviewService.getSuggestedTopics(limit = 8)
        topics = selectedSubjectId?.let { id -> DataManager.getTopics().filter { it.subjectId == id } } ?: emptyList()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SubjectSidebar(
            subjects = subjects,
            selectedId = selectedSubjectId,
            onSelect = { selectedSubjectId = it },
            onAdd = { showAddSubjectDialog = true }
        )

        Column(
            modifier = Modifier.weight(1.5f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TopicSuggestionBoard(
                suggestions = suggestions,
                onStart = {
                    try {
                        reviewService.markAsReviewed(it.topic.id)
                        suggestions = reviewService.getSuggestedTopics(limit = 8)
                        UiEventBus.notifyDataChanged()
                        // Navigate to Focus Timer screen
                        onNavigateToTimer()
                    } catch (e: Exception) {
                        println("Error marking topic as reviewed: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onSkip = {
                    try {
                        reviewService.skipTopic(it.topic.id)
                        suggestions = reviewService.getSuggestedTopics(limit = 8)
                        UiEventBus.notifyDataChanged()
                    } catch (e: Exception) {
                        println("Error skipping topic: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
            SubjectDetailCard(
                subjects = subjects,
                selectedSubjectId = selectedSubjectId,
                subject = subjects.find { it.id == selectedSubjectId },
                topics = topics,
                onSubjectChanged = { subjectId -> 
                    selectedSubjectId = subjectId
                },
                onAddTopic = { subjectId -> 
                    topicSubjectId = subjectId
                    showAddTopicDialog = true 
                },
                onEditTopic = { topic ->
                    editingTopic = topic
                }
            )
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onAdd = { name, hours, color ->
                val subject = Subject(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    targetHoursPerWeek = hours,
                    color = color
                )
                DataManager.addSubject(subject)
                selectedSubjectId = subject.id
                subjects = DataManager.getSubjects()
                UiEventBus.notifyDataChanged()
                showAddSubjectDialog = false
            }
        )
    }

    if (showAddTopicDialog) {
        AddTopicDialog(
            subjects = subjects,
            selectedSubjectId = topicSubjectId ?: selectedSubjectId,
            onDismiss = { 
                showAddTopicDialog = false
                topicSubjectId = null
            },
            onAdd = { name, subjectId, difficulty ->
                val topic = Topic(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    name = name,
                    difficulty = difficulty
                )
                DataManager.addTopic(topic)
                if (selectedSubjectId == subjectId) {
                    topics = DataManager.getTopics().filter { it.subjectId == subjectId }
                }
                UiEventBus.notifyDataChanged()
                showAddTopicDialog = false
                topicSubjectId = null
            }
        )
    }
    
    editingTopic?.let { topic ->
        EditTopicDialog(
            topic = topic,
            onDismiss = { editingTopic = null },
            onSave = { newName, newDifficulty ->
                val updatedTopic = topic.copy(
                    name = newName,
                    difficulty = newDifficulty
                )
                DataManager.updateTopic(updatedTopic)
                topics = DataManager.getTopics().filter { it.subjectId == selectedSubjectId }
                suggestions = reviewService.getSuggestedTopics(limit = 8)
                UiEventBus.notifyDataChanged()
                editingTopic = null
            }
        )
    }
}

@Composable
private fun EditTopicDialog(
    topic: Topic,
    onDismiss: () -> Unit,
    onSave: (name: String, difficulty: Int) -> Unit
) {
    var name by remember { mutableStateOf(topic.name) }
    var difficulty by remember { mutableStateOf(topic.difficulty.toFloat()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Topic", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("Difficulty: ${difficulty.toInt()}", fontWeight = FontWeight.Medium)
                Slider(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 (Easy)", style = MaterialTheme.typography.caption)
                    Text("10 (Hard)", style = MaterialTheme.typography.caption)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, difficulty.toInt()) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
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
private fun SubjectSidebar(
    subjects: List<Subject>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Subjects", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Subject")
        }
        
        val listState = rememberLazyListState()
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subjects) { subject ->
                    val selected = subject.id == selectedId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) MaterialTheme.colors.primary.copy(alpha = 0.15f) 
                                else MaterialTheme.colors.surface,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(subject.id) }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(subject.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                Text("${subject.targetHoursPerWeek} hrs / week", style = MaterialTheme.typography.body2)
                            }
                            IconButton(
                                onClick = {
                                    DataManager.deleteSubject(subject.id)
                                    // Also delete all topics for this subject
                                    DataManager.getTopics().filter { it.subjectId == subject.id }.forEach {
                                        DataManager.deleteTopic(it.id)
                                    }
                                    UiEventBus.notifyDataChanged()
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFE74C3C))
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

@Composable
private fun TopicSuggestionBoard(
    suggestions: List<ReviewSuggestionService.TopicSuggestion>,
    onStart: (ReviewSuggestionService.TopicSuggestion) -> Unit,
    onSkip: (ReviewSuggestionService.TopicSuggestion) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Review Queue", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            if (suggestions.isEmpty()) {
                Text("No topics pending review.")
            } else {
                suggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colors.surface,
                        elevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val subject = DataManager.getSubjects().find { it.id == suggestion.topic.subjectId }
                            Text(suggestion.topic.name, fontWeight = FontWeight.Bold)
                            Text(
                                subject?.name ?: "Unknown",
                                style = MaterialTheme.typography.caption
                            )
                            var priorityValue by remember(suggestion.topic.id) { 
                                mutableStateOf(suggestion.topic.manualPriority ?: (suggestion.priorityScore / 10.0).toFloat().coerceIn(0f, 10f))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Priority: ${String.format("%.1f", priorityValue)}", style = MaterialTheme.typography.caption)
                                Slider(
                                    value = priorityValue,
                                    onValueChange = { priorityValue = it },
                                    valueRange = 0f..10f,
                                    steps = 99
                                )
                                Button(
                                    onClick = {
                                        val updated = suggestion.topic.copy(manualPriority = priorityValue)
                                        DataManager.updateTopic(updated)
                                        // Refresh suggestions to reflect new priority
                                        // Note: This will be handled by the parent component's LaunchedEffect
                                        UiEventBus.notifyDataChanged()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Update Priority")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onStart(suggestion) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start Reviewing")
                                }
                                OutlinedButton(
                                    onClick = { onSkip(suggestion) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Skip")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectDetailCard(
    subjects: List<Subject>,
    selectedSubjectId: String?,
    subject: Subject?,
    topics: List<Topic>,
    onSubjectChanged: (String?) -> Unit,
    onAddTopic: (String?) -> Unit,
    onEditTopic: (Topic) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSubjectForTopic by remember { mutableStateOf<String?>(selectedSubjectId) }
    
    LaunchedEffect(selectedSubjectId) {
        selectedSubjectForTopic = selectedSubjectId
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 800.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        val selectedSubjectColor = subject?.let { hexToColor(it.color) } ?: Color(0xFF3498DB)
                        OutlinedTextField(
                            value = subject?.name ?: "Select Subject",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Subject") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = selectedSubjectColor.copy(alpha = 0.5f),
                                focusedBorderColor = selectedSubjectColor,
                                unfocusedBorderColor = selectedSubjectColor.copy(alpha = 0.8f),
                                focusedLabelColor = Color.White.copy(alpha = 0.9f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                textColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown, 
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            },
                            modifier = Modifier.width(200.dp)
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            subjects.forEach { subj ->
                                val subjectColor = hexToColor(subj.color)
                                DropdownMenuItem(
                                    onClick = {
                                        onSubjectChanged(subj.id)
                                        selectedSubjectForTopic = subj.id
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
                                                .background(subjectColor, CircleShape)
                                        )
                                        Text(subj.name)
                                    }
                                }
                            }
                        }
                    }
                    subject?.let {
                        Text(
                            "${it.targetHoursPerWeek} hr goal",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAddTopic(selectedSubjectForTopic ?: selectedSubjectId) },
                        enabled = selectedSubjectForTopic != null || selectedSubjectId != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Topic")
                    }
                    OutlinedButton(
                        onClick = {
                            val subjectToRemove = selectedSubjectForTopic ?: selectedSubjectId
                            subjectToRemove?.let { subjectId ->
                                DataManager.deleteSubject(subjectId)
                                // Also delete all topics for this subject
                                DataManager.getTopics().filter { it.subjectId == subjectId }.forEach {
                                    DataManager.deleteTopic(it.id)
                                }
                                UiEventBus.notifyDataChanged()
                                // Reset selection
                                onSubjectChanged(null)
                                selectedSubjectForTopic = null
                            }
                        },
                        enabled = selectedSubjectForTopic != null || selectedSubjectId != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C))
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove Subject", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Subject")
                    }
                }
            }

            if (subject == null) {
                Text("Choose a subject from the sidebar to manage topics.")
            } else if (topics.isEmpty()) {
                Text("No topics yet. Add one to start tracking reviews.")
            } else {
                val topicListState = rememberLazyListState()
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = topicListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topics) { topic ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colors.surface,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(topic.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Difficulty: ${topic.difficulty}/10",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onEditTopic(topic) }) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF2196F3)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                DataManager.deleteTopic(topic.id)
                                                UiEventBus.notifyDataChanged()
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFE74C3C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(topicListState)
                    )
                }
            }
        }
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("10") }
    var selectedColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color(0xFF3498DB)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = hours, onValueChange = { hours = it }, label = { Text("Target Hours/Week") })
                Text("Select Color", style = MaterialTheme.typography.subtitle2)
                ColorWheelPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hexColor = String.format("#%06X", (0xFFFFFF and selectedColor.value.toInt()))
                    onAdd(name, hours.toDoubleOrNull() ?: 10.0, hexColor)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddTopicDialog(
    subjects: List<Subject>,
    selectedSubjectId: String?,
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(5f) }
    var selectedSubject by remember { mutableStateOf(selectedSubjectId ?: subjects.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Subject dropdown
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val subjectName = subjects.find { it.id == selectedSubject }?.name ?: "Select Subject"
                        Text(subjectName, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedSubject = subject.id
                                    expanded = false
                                }
                            ) {
                                Text(subject.name)
                            }
                        }
                    }
                }
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Difficulty: ${difficulty.toInt()}", style = MaterialTheme.typography.body2)
                    Slider(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSubject?.let { onAdd(name, it, difficulty.toInt()) }
                },
                enabled = selectedSubject != null && name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ColorWheelPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = 200.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                val sizePx = with(density) { size.toPx() }
                detectTapGestures { tapOffset ->
                    val radius = sizePx / 2f
                    val centerX = radius
                    val centerY = radius
                    val dx = tapOffset.x - centerX
                    val dy = tapOffset.y - centerY
                    val distance = sqrt(dx * dx + dy * dy)
                    
                    if (distance <= radius) {
                        val angle: Double = atan2(dy.toDouble(), dx.toDouble())
                        val normalizedAngle: Double = if (angle < 0.0) angle + 2.0 * PI else angle
                        val twoPi: Double = 2.0 * PI
                        val hue: Float = ((normalizedAngle / twoPi) * 360.0).toFloat()
                        val saturation = (distance / radius).coerceIn(0f, 1f)
                        val value = 1f
                        
                        val color = hsvToColor(hue, saturation, value)
                        onColorSelected(color)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = this.size.minDimension
            val radius = sizePx / 2f
            val centerX = radius
            val centerY = radius
            
            // Draw color wheel using radial gradient approach
            for (angle in 0 until 360 step 2) {
                val hue = angle.toFloat()
                val radians = Math.toRadians(angle.toDouble())
                
                for (r in 0 until radius.toInt() step 2) {
                    val distance = r.toFloat() / radius
                    val saturation = distance
                    val value = 1f
                    
                    val color = hsvToColor(hue, saturation, value)
                    val x = centerX + (cos(radians) * r).toFloat()
                    val y = centerY + (sin(radians) * r).toFloat()
                    
                    drawCircle(
                        color = color,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }
            
            // Draw selected color indicator
            val (h, s, _) = colorToHsv(selectedColor)
            val selectedAngle = Math.toRadians(h.toDouble())
            val selectedDistance = s * radius
            val indicatorX = centerX + (cos(selectedAngle) * selectedDistance).toFloat()
            val indicatorY = centerY + (sin(selectedAngle) * selectedDistance).toFloat()
            
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = Offset(indicatorX, indicatorY),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = selectedColor,
                radius = 7f,
                center = Offset(indicatorX, indicatorY)
            )
        }
        
        // Show selected color preview
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp),
            shape = CircleShape,
            color = selectedColor,
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
        ) {}
    }
}

private fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta + (if (g < b) 6 else 0)) * 60f
        max == g -> ((b - r) / delta + 2) * 60f
        else -> ((r - g) / delta + 4) * 60f
    }
    
    val saturation = if (max == 0f) 0f else delta / max
    val value = max
    
    return Triple(hue, saturation, value)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - abs((h / 60f) % 2 - 1))
    val m = v - c
    
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return Color(r + m, g + m, b + m)
}

