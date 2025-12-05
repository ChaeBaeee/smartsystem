package com.smartstudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.smartstudy.data.DataManager
import com.smartstudy.models.Subject
import com.smartstudy.models.Topic
import com.smartstudy.services.ReviewSuggestionService
import com.smartstudy.ui.UiEventBus
import com.smartstudy.utils.hexToColor
import java.util.UUID



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

    val subjects = remember(dataVersion) { DataManager.getSubjects() }
    var suggestions by remember { mutableStateOf(reviewService.getSuggestedTopics(limit = 8)) }
    var topics by remember { mutableStateOf<List<Topic>>(emptyList()) }

    LaunchedEffect(selectedSubjectId, dataVersion) {
        if (selectedSubjectId == null && subjects.isNotEmpty()) {
            selectedSubjectId = subjects.first().id
        }
        suggestions = reviewService.getSuggestedTopics(limit = 8)
        topics = selectedSubjectId?.let { id -> DataManager.getTopics().filter { it.subjectId == id } } ?: emptyList()
    }

    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        onSubjectChanged = { subjectId: String? -> 
                            selectedSubjectId = subjectId
                        },
                        onAddTopic = { subjectId: String? -> 
                            topicSubjectId = subjectId
                            showAddTopicDialog = true 
                        },
                        onEditTopic = { topic: Topic ->
                            editingTopic = topic
                        }
                    )
                }
            }
        }
        
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { },
                onAdd = { name: String, hours: Double, color: String ->
                val subject = Subject(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    targetHoursPerWeek = hours,
                    color = color
                )
                DataManager.addSubject(subject)
                selectedSubjectId = subject.id
                UiEventBus.notifyDataChanged()
            }
        )
    }

    if (showAddTopicDialog) {
        AddTopicDialog(
            subjects = subjects,
            selectedSubjectId = topicSubjectId ?: selectedSubjectId,
            onDismiss = { },
            onAdd = { name: String, subjectId: String, difficulty: Int ->
                val topic = Topic(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    name = name.trim(),
                    difficulty = difficulty
                )
                DataManager.addTopic(topic)
                if (selectedSubjectId == subjectId) {
                    topics = DataManager.getTopics().filter { it.subjectId == subjectId }
                }
                UiEventBus.notifyDataChanged()
            }
        )
    }
    
    editingTopic?.let { topic ->
        EditTopicDialog(
            topic = topic,
            onDismiss = { },
            onSave = { newName, newDifficulty, newSubjectId ->
                val updatedTopic = topic.copy(
                    name = newName.trim(),
                    difficulty = newDifficulty,
                    subjectId = newSubjectId
                )
                DataManager.updateTopic(updatedTopic)
                topics = DataManager.getTopics().filter { it.subjectId == selectedSubjectId }
                suggestions = reviewService.getSuggestedTopics(limit = 8)
                UiEventBus.notifyDataChanged()
            }
        )
    }
}

@Composable
private fun EditTopicDialog(
    topic: Topic,
    onDismiss: () -> Unit,
    onSave: (name: String, difficulty: Int, subjectId: String) -> Unit
) {
    val subjects = DataManager.getSubjects()
    var name by remember { mutableStateOf(topic.name) }
    var difficulty by remember { mutableStateOf(topic.difficulty.toFloat()) }
    var selectedSubjectId by remember { mutableStateOf(topic.subjectId) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Topic", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Subject dropdown
                Text("Subject", fontWeight = FontWeight.Medium)
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val subjectName = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject"
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val subjectColor = subjects.find { it.id == selectedSubjectId }?.let { hexToColor(it.color) } ?: Color(0xFF3498DB)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(subjectColor, CircleShape)
                            )
                            Text(subjectName)
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { subject ->
                            val subjectColor = hexToColor(subject.color)
                            DropdownMenuItem(
                                onClick = {
                                    selectedSubjectId = subject.id
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
                                    Text(subject.name)
                                }
                            }
                        }
                    }
                }
                
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
                onClick = { onSave(name.trim(), difficulty.toInt(), selectedSubjectId) },
                enabled = name.isNotBlank() && selectedSubjectId.isNotEmpty(),
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
            .width(320.dp),
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
        
        if (subjects.isEmpty()) {
            Text(
                "No subjects yet. Click 'Add Subject' to create one.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.heightIn(min = 200.dp, max = 600.dp)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = subjects,
                        key = { it.id }
                    ) { subject ->
                    val selected = subject.id == selectedId
                    val subjectColor = hexToColor(subject.color)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                subjectColor,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(subject.id) }
                            .border(
                                width = 1.dp,
                                color = if (selected) Color.White else subjectColor.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = subject.name, 
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        style = MaterialTheme.typography.body1
                                    )
                                    Text(
                                        text = "${subject.targetHoursPerWeek} hrs / week", 
                                        style = MaterialTheme.typography.body2,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
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
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                        .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete, 
                                        contentDescription = "Delete", 
                                        tint = Color(0xFFE74C3C),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(18.dp)
                                    )
                                }
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

@Composable
private fun TopicSuggestionBoard(
    suggestions: List<ReviewSuggestionService.TopicSuggestion>,
    onStart: (ReviewSuggestionService.TopicSuggestion) -> Unit,
    onSkip: (ReviewSuggestionService.TopicSuggestion) -> Unit
) {
    val allTopics = DataManager.getTopics()
    val allSubjects = DataManager.getSubjects()
    val reviewService = remember { ReviewSuggestionService() }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var selectedDifficultyFilter by remember { mutableStateOf<Int?>(null) }
    var sortBy by remember { mutableStateOf("priority") } // priority, date, difficulty, reviewCount
    var showManualSelect by remember { mutableStateOf(false) }
    var manuallySelectedTopicId by remember { mutableStateOf<String?>(null) }
    var editingTopicId by remember { mutableStateOf<String?>(null) }
    var editingTopicName by remember { mutableStateOf("") }
    var editingTopicDifficulty by remember { mutableStateOf(5f) }
    
    // Get filtered and sorted suggestions
    // When filters are applied, generate suggestions from all topics instead of limited suggestions
    val filteredSuggestions = remember(suggestions, searchQuery, selectedSubjectFilter, selectedDifficultyFilter, sortBy, allTopics) {
        // If filters are active, generate suggestions from all topics
        val baseSuggestions = if (selectedSubjectFilter != null || selectedDifficultyFilter != null || searchQuery.isNotBlank()) {
            // Generate suggestions from all topics when filters are active
            val topicsToUse = allTopics.filter { topic ->
                (selectedSubjectFilter == null || topic.subjectId == selectedSubjectFilter) &&
                (selectedDifficultyFilter == null || topic.difficulty == selectedDifficultyFilter) &&
                (searchQuery.isBlank() || 
                 topic.name.contains(searchQuery, ignoreCase = true) ||
                 allSubjects.find { s -> s.id == topic.subjectId }?.name?.contains(searchQuery, ignoreCase = true) == true)
            }
            
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            
            topicsToUse.mapNotNull { topic ->
                val daysSinceReview = if (topic.lastReviewed != null) {
                    val days = (now - topic.lastReviewed) / oneDay
                    if (days < 0) return@mapNotNull null
                    days
                } else {
                    Long.MAX_VALUE
                }
                
                // Calculate priority score
                val score = topic.manualPriority?.toDouble()?.times(10.0) ?: run {
                    var s = 0.0
                    s += daysSinceReview.coerceAtMost(30) * 2.0
                    s += (10 - topic.reviewCount.coerceAtMost(10)) * 1.5
                    s += topic.difficulty * 1.0
                    if (topic.lastReviewed == null) s += 50.0
                    s
                }
                
                ReviewSuggestionService.TopicSuggestion(topic, score, daysSinceReview)
            }
        } else {
            suggestions
        }
        
        var filtered = baseSuggestions.toMutableList()
        
        // Apply search query filter if not already applied
        if (searchQuery.isNotBlank() && (selectedSubjectFilter == null && selectedDifficultyFilter == null)) {
            filtered = filtered.filter { 
                it.topic.name.contains(searchQuery, ignoreCase = true) ||
                allSubjects.find { s -> s.id == it.topic.subjectId }?.name?.contains(searchQuery, ignoreCase = true) == true
            }.toMutableList()
        }
        
        // Apply subject filter if not already applied
        if (selectedSubjectFilter != null && selectedDifficultyFilter == null && searchQuery.isBlank()) {
            filtered = filtered.filter { it.topic.subjectId == selectedSubjectFilter }.toMutableList()
        }
        
        // Apply difficulty filter if not already applied
        if (selectedDifficultyFilter != null && selectedSubjectFilter == null && searchQuery.isBlank()) {
            filtered = filtered.filter { it.topic.difficulty == selectedDifficultyFilter }.toMutableList()
        }
        
        // Sort
        filtered = when (sortBy) {
            "priority" -> filtered.sortedByDescending { it.priorityScore }.toMutableList()
            "date" -> filtered.sortedByDescending { it.topic.lastReviewed ?: 0L }.toMutableList()
            "difficulty" -> filtered.sortedByDescending { it.topic.difficulty }.toMutableList()
            "reviewCount" -> filtered.sortedByDescending { it.topic.reviewCount }.toMutableList()
            else -> filtered
        }
        
        filtered
    }
    
    // Get manually selected topic suggestion if any
    val manuallySelectedSuggestion = manuallySelectedTopicId?.let { topicId ->
        val topic = allTopics.find { it.id == topicId }
        topic?.let { t ->
            val daysSince = if (t.lastReviewed != null) {
                (System.currentTimeMillis() - t.lastReviewed) / (24 * 60 * 60 * 1000L)
            } else Long.MAX_VALUE
            ReviewSuggestionService.TopicSuggestion(t, reviewService.getSuggestedTopics().find { it.topic.id == topicId }?.priorityScore ?: 0.0, daysSince)
        }
    }
    
    // Combine auto suggestions with manually selected
    val displaySuggestions = remember(filteredSuggestions, manuallySelectedSuggestion) {
        val combined = filteredSuggestions.toMutableList()
        manuallySelectedSuggestion?.let {
            if (!combined.any { s -> s.topic.id == it.topic.id }) {
                combined.add(0, it)
            }
        }
        combined
    }
    
    Card(
        modifier = Modifier.heightIn(max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and manual select button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Review Queue", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { showManualSelect = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Select Topic")
                }
            }
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search topics...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Filters and sort row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subject filter
                var subjectFilterExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { subjectFilterExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            selectedSubjectFilter?.let { allSubjects.find { s -> s.id == it }?.name } ?: "All Subjects",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = subjectFilterExpanded,
                        onDismissRequest = { subjectFilterExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = { selectedSubjectFilter = null; subjectFilterExpanded = false }) {
                            Text("All Subjects")
                        }
                        allSubjects.forEach { subject ->
                            val subjectColor = hexToColor(subject.color)
                            DropdownMenuItem(
                                onClick = { selectedSubjectFilter = subject.id; subjectFilterExpanded = false }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(subjectColor, CircleShape)
                                    )
                                    Text(subject.name)
                                }
                            }
                        }
                    }
                }
                
                // Difficulty filter
                var difficultyFilterExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { difficultyFilterExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            selectedDifficultyFilter?.toString() ?: "All Difficulties",
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = difficultyFilterExpanded,
                        onDismissRequest = { difficultyFilterExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = { selectedDifficultyFilter = null; difficultyFilterExpanded = false }) {
                            Text("All Difficulties")
                        }
                        (1..10).forEach { diff ->
                            DropdownMenuItem(onClick = { selectedDifficultyFilter = diff; difficultyFilterExpanded = false }) {
                                Text("$diff/10")
                            }
                        }
                    }
                }
                
                // Sort
                var sortExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { sortExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (sortBy) {
                                "priority" -> "Priority"
                                "date" -> "Date"
                                "difficulty" -> "Difficulty"
                                "reviewCount" -> "Reviews"
                                else -> "Sort"
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = { sortBy = "priority"; sortExpanded = false }) {
                            Text("By Priority")
                        }
                        DropdownMenuItem(onClick = { sortBy = "date"; sortExpanded = false }) {
                            Text("By Date")
                        }
                        DropdownMenuItem(onClick = { sortBy = "difficulty"; sortExpanded = false }) {
                            Text("By Difficulty")
                        }
                        DropdownMenuItem(onClick = { sortBy = "reviewCount"; sortExpanded = false }) {
                            Text("By Review Count")
                        }
                    }
                }
            }
            
            // Topics list
            if (displaySuggestions.isEmpty()) {
                Text("No topics found.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            } else {
                val reviewQueueListState = rememberLazyListState()
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = reviewQueueListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displaySuggestions) { suggestion ->
                    val subject = allSubjects.find { it.id == suggestion.topic.subjectId }
                    val subjectColor = subject?.let { hexToColor(it.color) } ?: Color(0xFF3498DB)
                    val isEditing = editingTopicId == suggestion.topic.id
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val daysSinceReview = suggestion.daysSinceReview
                    val lastReviewedText = if (suggestion.topic.lastReviewed != null) {
                        if (daysSinceReview < 1) "Today"
                        else if (daysSinceReview == 1L) "Yesterday"
                        else if (daysSinceReview < 7) "$daysSinceReview days ago"
                        else dateFormat.format(Date(suggestion.topic.lastReviewed))
                    } else "Never reviewed"
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = subjectColor.copy(alpha = 0.1f),
                        elevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Topic header with edit button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editingTopicName,
                                            onValueChange = { editingTopicName = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            label = { Text("Topic Name") }
                                        )
                                    } else {
                                        Text(suggestion.topic.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(subjectColor, CircleShape)
                                        )
                                        Text(
                                            subject?.name ?: "Unknown",
                                            style = MaterialTheme.typography.caption,
                                            color = subjectColor
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (isEditing) {
                                            // Save edit
                                            val updated = suggestion.topic.copy(
                                                name = editingTopicName.trim(),
                                                difficulty = editingTopicDifficulty.toInt()
                                            )
                                            DataManager.updateTopic(updated)
                                            UiEventBus.notifyDataChanged()
                                            editingTopicId = null
                                        } else {
                                            // Start editing
                                            editingTopicId = suggestion.topic.id
                                            editingTopicName = suggestion.topic.name
                                            editingTopicDifficulty = suggestion.topic.difficulty.toFloat()
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isEditing) Icons.Filled.PlayArrow else Icons.Filled.Edit,
                                        contentDescription = if (isEditing) "Save" else "Edit",
                                        tint = if (isEditing) Color(0xFF27AE60) else Color(0xFF2196F3)
                                    )
                                }
                            }
                            
                            // More information display
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Last reviewed: $lastReviewedText", style = MaterialTheme.typography.caption)
                                if (suggestion.topic.lastReviewed == null) {
                                    Surface(
                                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "New Topic - Never Reviewed",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                }
                            }
                            
                            // Difficulty (editable if editing)
                            if (isEditing) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Difficulty: ${editingTopicDifficulty.toInt()}", fontWeight = FontWeight.Medium)
                                    Slider(
                                        value = editingTopicDifficulty,
                                        onValueChange = { editingTopicDifficulty = it },
                                        valueRange = 1f..10f,
                                        steps = 8,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                Text("Difficulty: ${suggestion.topic.difficulty}/10", style = MaterialTheme.typography.caption)
                            }
                            
                            // Priority slider
                            var priorityValue by remember(suggestion.topic.id) { 
                                mutableStateOf(suggestion.topic.manualPriority ?: (suggestion.priorityScore / 10.0).toFloat().coerceIn(0f, 10f))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Priority: ${String.format("%.1f", priorityValue)}", style = MaterialTheme.typography.caption)
                                Slider(
                                    value = priorityValue,
                                    onValueChange = { priorityValue = it },
                                    valueRange = 0f..10f,
                                    steps = 99,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val updated = suggestion.topic.copy(manualPriority = priorityValue)
                                        DataManager.updateTopic(updated)
                                        UiEventBus.notifyDataChanged()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Update Priority")
                                }
                            }
                            
                            // Quick actions row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Mark Easy/Hard buttons
                                OutlinedButton(
                                    onClick = {
                                        val newDiff = if (suggestion.topic.difficulty > 5) 3 else 8
                                        val updated = suggestion.topic.copy(difficulty = newDiff)
                                        DataManager.updateTopic(updated)
                                        UiEventBus.notifyDataChanged()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (suggestion.topic.difficulty > 5) "Easy" else "Hard",
                                        style = MaterialTheme.typography.caption
                                    )
                                }
                            }
                            
                            // Main action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onStart(suggestion) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
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
                            
                            // Remove from queue button (if manually selected)
                            if (manuallySelectedTopicId == suggestion.topic.id) {
                                OutlinedButton(
                                    onClick = {
                                        manuallySelectedTopicId = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C))
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove from Queue")
                                }
                            }
                        }
                    }
                    }
                    }
                    
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(reviewQueueListState)
                    )
                }
            }
        }
    }
    
    // Manual topic selection dialog
    if (showManualSelect) {
        var searchText by remember { mutableStateOf("") }
        val availableTopics = remember(searchText) {
            allTopics.filter { topic ->
                searchText.isBlank() || topic.name.contains(searchText, ignoreCase = true) ||
                allSubjects.find { s -> s.id == topic.subjectId }?.name?.contains(searchText, ignoreCase = true) == true
            }
        }
        
        AlertDialog(
            onDismissRequest = { showManualSelect = false },
            title = { Text("Select Topic to Review", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search topics...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true
                    )
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn(state = listState) {
                            items(availableTopics) { topic ->
                                val topicSubject = allSubjects.find { it.id == topic.subjectId }
                                val topicSubjectColor = topicSubject?.let { hexToColor(it.color) } ?: Color(0xFF3498DB)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            manuallySelectedTopicId = topic.id
                                            showManualSelect = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = topicSubjectColor.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, topicSubjectColor.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(topicSubjectColor, CircleShape)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(topic.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                topicSubject?.name ?: "Unknown",
                                                style = MaterialTheme.typography.caption
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualSelect = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
}

@Composable
fun SubjectDetailCard(
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
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fixed header with Add Topic button
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
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "Remove Subject", 
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Subject")
                    }
                }
            }

            // Scrollable topics list
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
                            val topicSubjectColor = hexToColor(subject.color)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        topicSubjectColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = topicSubjectColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
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
                                                .background(topicSubjectColor, CircleShape)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(topic.name, fontWeight = FontWeight.Bold, color = topicSubjectColor)
                                            Text(
                                                "Difficulty: ${topic.difficulty}/10",
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
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
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                                    .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFFE74C3C),
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(18.dp)
                                                )
                                            }
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
                    onColorSelected = { color: Color -> selectedColor = color }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Convert Color to hex string properly
                    val r = (selectedColor.red * 255).toInt()
                    val g = (selectedColor.green * 255).toInt()
                    val b = (selectedColor.blue * 255).toInt()
                    val hexColor = String.format("#%02X%02X%02X", r, g, b)
                    onAdd(name.trim(), hours.toDoubleOrNull() ?: 10.0, hexColor)
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
                            val subjectColor = hexToColor(subject.color)
                            DropdownMenuItem(
                                onClick = {
                                    selectedSubject = subject.id
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
                                    Text(subject.name)
                                }
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
fun ColorWheelPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = 200.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    Box(
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
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
            val selectedDistance = (s * radius).toFloat()
            val indicatorX = centerX + (cos(selectedAngle) * selectedDistance.toDouble()).toFloat()
            val indicatorY = centerY + (sin(selectedAngle) * selectedDistance.toDouble()).toFloat()
            
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

fun colorToHsv(color: Color): Triple<Float, Float, Float> {
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

fun hsvToColor(h: Float, s: Float, v: Float): Color {
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


