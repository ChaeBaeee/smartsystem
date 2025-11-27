package com.smartstudy.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartstudy.data.DataManager
import com.smartstudy.models.Grade
import com.smartstudy.services.PerformanceMonitoringService
import java.text.SimpleDateFormat
import java.util.*
import com.smartstudy.ui.components.StatPill
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

data class GradeStats(
    val averagePercentage: Double,
    val totalAssignments: Int,
    val bestSubject: String?,
    val worstSubject: String?
)

fun computeGradeStats(grades: List<Grade>, subjects: List<com.smartstudy.models.Subject>): GradeStats {
    if (grades.isEmpty()) return GradeStats(0.0, 0, null, null)
    val average = grades.map { it.score / it.maxScore * 100 }.average()
    val grouped = grades.groupBy { it.subjectId }.mapValues { entry ->
        entry.value.map { it.score / it.maxScore * 100 }.average()
    }
    val best = grouped.maxByOrNull { it.value }?.key?.let { id -> subjects.find { it.id == id }?.name }
    val worst = grouped.minByOrNull { it.value }?.key?.let { id -> subjects.find { it.id == id }?.name }
    return GradeStats(average, grades.size, best, worst)
}

@Composable
fun GradesScreen() {
    val performanceService = remember { PerformanceMonitoringService() }
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy") }
    val dataVersion = UiEventBus.dataVersion

    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var showAddGradeDialog by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<Grade?>(null) }
    var gradeVersion by remember { mutableStateOf(0) }

    fun loadGrades(subjectName: String?): List<Grade> {
        return if (subjectName == null || subjectName == "All Subjects") {
            DataManager.getGrades()
        } else {
            val subject = DataManager.getSubjects().find { it.name == subjectName }
            if (subject != null) {
                DataManager.getGrades().filter { it.subjectId == subject.id }
            } else {
                emptyList()
            }
        }.sortedByDescending { it.date }
    }

    val subjectsSnapshot = remember(gradeVersion, dataVersion) { DataManager.getSubjects() }
    val grades = remember(selectedSubject, gradeVersion, dataVersion) {
        loadGrades(selectedSubject)
    }
    val allPerformance = remember(gradeVersion, dataVersion) { performanceService.getAllSubjectPerformance() }
    val gradeStats = remember(gradeVersion, dataVersion, grades, subjectsSnapshot) { 
        computeGradeStats(grades, subjectsSnapshot) 
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
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
                Text("Grades & Performance", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                Text("Track assessments and see trends per subject", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            }
            Button(onClick = { showAddGradeDialog = true }, shape = RoundedCornerShape(12.dp)) {
                Text("Add Grade")
            }
        }

        // Key metrics in colorful boxes
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF6E8BFF).copy(alpha = 0.15f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Overall Average", style = MaterialTheme.typography.caption, color = Color(0xFF6E8BFF))
                    Text(
                        "${String.format("%.1f", gradeStats.averagePercentage)}%",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6E8BFF)
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF6FEDD6).copy(alpha = 0.15f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Grades", style = MaterialTheme.typography.caption, color = Color(0xFF6FEDD6))
                    Text(
                        gradeStats.totalAssignments.toString(),
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6FEDD6)
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Best Subject", style = MaterialTheme.typography.caption, color = Color(0xFF4CAF50))
                    Text(
                        gradeStats.bestSubject ?: "—",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFF5F6D).copy(alpha = 0.15f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Worst Subject", style = MaterialTheme.typography.caption, color = Color(0xFFFF5F6D))
                    Text(
                        gradeStats.worstSubject ?: "—",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5F6D)
                    )
                }
            }
        }

        FilterRow(
            subjects = subjectsSnapshot.map { it.name },
            selected = selectedSubject,
            onSelected = { selectedSubject = it },
            onRefresh = { gradeVersion++ }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AverageBySubjectCard(allPerformance = allPerformance, subjects = subjectsSnapshot, modifier = Modifier.weight(0.9f))
            GradesListCard(
                grades = grades,
                subjects = subjectsSnapshot,
                dateFormat = dateFormat,
                onEditGrade = { grade: Grade ->
                    editingGrade = grade
                    showAddGradeDialog = true
                },
                modifier = Modifier.weight(1.1f)
            )
        }
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

    if (showAddGradeDialog) {
        AddGradeDialog(
            editingGrade = editingGrade,
            onDismiss = {
                showAddGradeDialog = false
                editingGrade = null
            },
            onAdd = { subjectName: String, type: String, score: Double, maxScore: Double, category: String ->
                val subject = DataManager.getSubjects().find { it.name == subjectName }
                if (subject != null) {
                    if (editingGrade != null) {
                        // Update existing grade
                        val updated = editingGrade!!.copy(
                            subjectId = subject.id,
                            type = type,
                            score = score,
                            maxScore = maxScore,
                            category = category
                        )
                        DataManager.updateGrade(updated)
                        scope.launch { snackbarHostState.showSnackbar("Grade updated") }
                    } else {
                        // Add new grade
                        val grade = Grade(
                            id = UUID.randomUUID().toString(),
                            subjectId = subject.id,
                            type = type,
                            score = score,
                            maxScore = maxScore,
                            date = System.currentTimeMillis(),
                            category = category
                        )
                        DataManager.addGrade(grade)
                        scope.launch { snackbarHostState.showSnackbar("Grade saved") }
                    }
                    gradeVersion++
                    UiEventBus.notifyDataChanged()
                    showAddGradeDialog = false
                    editingGrade = null
                }
            }
        )
    }
}

@Composable
private fun FilterRow(
    subjects: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf("All Subjects") + subjects
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(selected ?: "All Subjects", modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            onSelected(if (item == "All Subjects") null else item)
                            expanded = false
                        }
                    ) {
                        Text(item)
                    }
                }
            }
        }
        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
            Text("Apply Filter")
        }
    }
}

@Composable
private fun AverageBySubjectCard(
    allPerformance: List<PerformanceMonitoringService.SubjectPerformance>,
    subjects: List<com.smartstudy.models.Subject>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF5F7FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Averages by Subject", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Text("${allPerformance.size} subjects", style = MaterialTheme.typography.caption, color = Color(0xFF4A4A4A))
            }
            if (allPerformance.isEmpty() || allPerformance.all { it.averageGrade == null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No grades recorded yet",
                        style = MaterialTheme.typography.body2,
                        color = Color(0xFF4A4A4A)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allPerformance) { perf ->
                        val subject = subjects.find { it.id == perf.subjectId }
                        if (subject != null && perf.averageGrade != null) {
                            val subjectColor = subject.color?.let { colorStr ->
                                try {
                                    // Parse hex color string (e.g., "#FF0000" or "FF0000")
                                    val hex = if (colorStr.startsWith("#")) colorStr.substring(1) else colorStr
                                    val colorInt = hex.toLong(16).toInt()
                                    Color(colorInt or 0xFF000000.toInt())
                                } catch (e: Exception) {
                                    Color(0xFF6E8BFF)
                                }
                            } ?: Color(0xFF6E8BFF)
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        subjectColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(subject.name, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                                        Text("Status: ${perf.status}", style = MaterialTheme.typography.caption, color = Color(0xFF4A4A4A))
                                    }
                                    Text(
                                        "${String.format("%.1f", perf.averageGrade)}%",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1A1A)
                                    )
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
fun GradesListCard(
    grades: List<Grade>,
    subjects: List<com.smartstudy.models.Subject>,
    dateFormat: SimpleDateFormat,
    onEditGrade: (Grade) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    var showScrollUp by remember { mutableStateOf(false) }
    var showScrollDown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.layoutInfo.totalItemsCount) {
        showScrollUp = scrollState.firstVisibleItemIndex > 0
        showScrollDown = scrollState.firstVisibleItemIndex < scrollState.layoutInfo.totalItemsCount - 1
    }

    Box(modifier = modifier.fillMaxWidth().heightIn(min = 200.dp, max = 600.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF5F7FA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Grades", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text(
                        "${grades.size} entries",
                        style = MaterialTheme.typography.caption,
                        color = Color(0xFF4A4A4A)
                    )
                }
                if (grades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No grades recorded yet. Add your first grade to get started!",
                            style = MaterialTheme.typography.body2,
                            color = Color(0xFF4A4A4A)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = scrollState
                    ) {
                        items(grades) { grade ->
                            val subject = subjects.find { it.id == grade.subjectId }
                            val subjectColor = subject?.color?.let { colorStr ->
                                try {
                                    // Parse hex color string (e.g., "#FF0000" or "FF0000")
                                    val hex = if (colorStr.startsWith("#")) colorStr.substring(1) else colorStr
                                    val colorInt = hex.toLong(16).toInt()
                                    Color(colorInt or 0xFF000000.toInt())
                                } catch (e: Exception) {
                                    MaterialTheme.colors.primary
                                }
                            } ?: MaterialTheme.colors.primary

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        subjectColor.copy(alpha = 0.15f),
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
                                        Text(
                                            subject?.name ?: "Unknown",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1A1A)
                                        )
                                        Text(
                                            grade.type,
                                            style = MaterialTheme.typography.subtitle1,
                                            color = Color(0xFF2D2D2D)
                                        )
                                        Text(
                                            "Score: ${grade.score}/${grade.maxScore} • ${
                                                String.format(
                                                    "%.1f",
                                                    grade.score / grade.maxScore * 100
                                                )
                                            }%",
                                            color = Color(0xFF1A1A1A)
                                        )
                                        Text(
                                            "Date: ${dateFormat.format(Date(grade.date))}",
                                            style = MaterialTheme.typography.caption,
                                            color = Color(0xFF4A4A4A)
                                        )
                                        if (grade.category.isNotEmpty()) {
                                            Text(
                                                "Category: ${grade.category}",
                                                style = MaterialTheme.typography.caption,
                                                color = Color(0xFF4A4A4A)
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                // Edit grade - open dialog with pre-filled values
                                                onEditGrade(grade)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF2196F3)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                DataManager.deleteGrade(grade.id)
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
                }
            }
        }

        // Scroll buttons
        if (showScrollUp && grades.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        scrollState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                backgroundColor = Color(0xFF6E8BFF),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Scroll Up")
            }
        }
        if (showScrollDown && grades.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                backgroundColor = Color(0xFF6E8BFF),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Scroll Down")
            }
        }
    }
}

@Composable
fun AddGradeDialog(
    editingGrade: Grade? = null,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, String) -> Unit
) {
    val subject = editingGrade?.let { DataManager.getSubjects().find { s -> s.id == it.subjectId } }
    var subjectName by remember(editingGrade) { mutableStateOf(subject?.name) }
    var type by remember(editingGrade) { mutableStateOf(editingGrade?.type ?: "") }
    var score by remember(editingGrade) { mutableStateOf(editingGrade?.score?.toString() ?: "") }
    var maxScore by remember(editingGrade) { mutableStateOf(editingGrade?.maxScore?.toString() ?: "") }
    var category by remember(editingGrade) { mutableStateOf(editingGrade?.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingGrade != null) "Edit Grade" else "Add Grade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val subjects = DataManager.getSubjects().map { it.name }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(subjectName ?: "Select Subject", modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { item ->
                            DropdownMenuItem(
                                onClick = {
                                    subjectName = item
                                    expanded = false
                                }
                            ) {
                                Text(item)
                            }
                        }
                    }
                }
                TextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type") }
                )
                TextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Score") }
                )
                TextField(
                    value = maxScore,
                    onValueChange = { maxScore = it },
                    label = { Text("Max Score") }
                )
                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName != null) {
                        onAdd(
                            subjectName!!,
                            type,
                            score.toDoubleOrNull() ?: 0.0,
                            maxScore.toDoubleOrNull() ?: 100.0,
                            category
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

