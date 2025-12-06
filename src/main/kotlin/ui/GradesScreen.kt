package com.smartstudy.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartstudy.data.DataManager
import com.smartstudy.models.Grade
import com.smartstudy.services.PerformanceMonitoringService
import java.text.SimpleDateFormat
import java.util.*
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
                Text("Track assessments and see trends per subject", style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
            }
            Button(onClick = { showAddGradeDialog = true }, shape = RoundedCornerShape(12.dp)) {
                Text("Add Grade", style = MaterialTheme.typography.body1)
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
                    Text("Overall Average", style = MaterialTheme.typography.caption.copy(color = Color(0xFF6E8BFF)))
                    Text(
                        text = "${String.format("%.1f", gradeStats.averagePercentage)}%",
                        style = MaterialTheme.typography.h5.copy(color = Color(0xFF6E8BFF)),
                        fontWeight = FontWeight.Bold
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
                    Text("Total Grades", style = MaterialTheme.typography.caption.copy(color = Color(0xFF6FEDD6)))
                    Text(
                        text = gradeStats.totalAssignments.toString(),
                        style = MaterialTheme.typography.h5.copy(color = Color(0xFF6FEDD6)),
                        fontWeight = FontWeight.Bold
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
                    Text("Best Subject", style = MaterialTheme.typography.caption.copy(color = Color(0xFF4CAF50)))
                    Text(
                        text = gradeStats.bestSubject ?: "—",
                        style = MaterialTheme.typography.h5.copy(color = Color(0xFF4CAF50)),
                        fontWeight = FontWeight.Bold
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
                    Text("Worst Subject", style = MaterialTheme.typography.caption.copy(color = Color(0xFFFF5F6D)))
                    Text(
                        text = gradeStats.worstSubject ?: "—",
                        style = MaterialTheme.typography.h5.copy(color = Color(0xFFFF5F6D)),
                        fontWeight = FontWeight.Bold
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
                }
                showAddGradeDialog = false
                editingGrade = null
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
                Text(selected ?: "All Subjects", style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
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
                        Text(item, style = MaterialTheme.typography.body1)
                    }
                }
            }
        }
        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
            Text("Apply Filter", style = MaterialTheme.typography.body1)
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
                Text("Averages by Subject", style = MaterialTheme.typography.h6.copy(color = Color(0xFF1A1A1A)), fontWeight = FontWeight.Bold)
                Text("${allPerformance.size} subjects", style = MaterialTheme.typography.caption.copy(color = Color(0xFF4A4A4A)))
            }
            if (allPerformance.isEmpty() || allPerformance.all { it.averageGrade == null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No grades recorded yet",
                        style = MaterialTheme.typography.body2.copy(color = Color(0xFF4A4A4A))
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
                            val subjectColor = try {
                                // Parse hex color string (e.g., "#FF0000" or "FF0000")
                                val hex = if (subject.color.startsWith("#")) subject.color.substring(1) else subject.color
                                val colorInt = hex.toLong(16).toInt()
                                Color(colorInt or 0xFF000000.toInt())
                                } catch (_: Exception) {
                                    Color(0xFF6E8BFF)
                                }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        subjectColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = subjectColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(subject.name, style = MaterialTheme.typography.body1.copy(color = subjectColor), fontWeight = FontWeight.Bold)
                                        Text("Status: ${perf.status}", style = MaterialTheme.typography.caption.copy(color = Color(0xFF4A4A4A)))
                                    }
                                    Text(
                                        text = "${String.format("%.1f", perf.averageGrade)}%",
                                        style = MaterialTheme.typography.h6.copy(color = Color(0xFF1A1A1A)),
                                        fontWeight = FontWeight.Bold
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
                    Text("Recent Grades", style = MaterialTheme.typography.h6.copy(color = Color(0xFF1A1A1A)), fontWeight = FontWeight.Bold)
                    Text(
                        text = "${grades.size} entries",
                        style = MaterialTheme.typography.caption.copy(color = Color(0xFF4A4A4A))
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
                            text = "No grades recorded yet. Add your first grade to get started!",
                            style = MaterialTheme.typography.body2.copy(color = Color(0xFF4A4A4A))
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
                                    .border(
                                        width = 2.dp,
                                        color = subjectColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
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
                                            text = subject?.name ?: "Unknown",
                                            style = MaterialTheme.typography.body1.copy(color = subjectColor),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = grade.type,
                                            style = MaterialTheme.typography.subtitle1.copy(color = Color(0xFF2D2D2D))
                                        )
                                        Text(
                                            text = "Score: ${grade.score}/${grade.maxScore} • ${
                                                String.format(
                                                    "%.1f",
                                                    grade.score / grade.maxScore * 100
                                                )
                                            }%",
                                            style = MaterialTheme.typography.body2.copy(color = Color(0xFF1A1A1A))
                                        )
                                        Text(
                                            text = "Date: ${dateFormat.format(Date(grade.date))}",
                                            style = MaterialTheme.typography.caption.copy(color = Color(0xFF4A4A4A))
                                        )
                                        if (grade.category.isNotEmpty()) {
                                            Text(
                                                text = "Category: ${grade.category}",
                                                style = MaterialTheme.typography.caption.copy(color = Color(0xFF4A4A4A))
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

    // Validation states
    val scoreValue = score.toDoubleOrNull()
    val maxScoreValue = maxScore.toDoubleOrNull()
    val isScoreValid = score.isEmpty() || scoreValue != null
    val isMaxScoreValid = maxScore.isEmpty() || maxScoreValue != null
    val isMaxScoreGreaterOrEqual = scoreValue != null && maxScoreValue != null && maxScoreValue >= scoreValue
    val canSubmit = subjectName != null && 
                    type.isNotBlank() && 
                    scoreValue != null && 
                    maxScoreValue != null && 
                    isMaxScoreGreaterOrEqual

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (editingGrade != null) "Edit Grade" else "Add Grade",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(subjectName ?: "Select Subject", style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DataManager.getSubjects().forEach { subject ->
                            val subjectColor = try {
                                val hex = if (subject.color.startsWith("#")) subject.color.substring(1) else subject.color
                                val colorInt = hex.toLong(16).toInt()
                                Color(colorInt or 0xFF000000.toInt())
                                } catch (_: Exception) {
                                    Color(0xFF6E8BFF)
                                }
                            DropdownMenuItem(
                                onClick = {
                                    subjectName = subject.name
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
                                    Text(subject.name, style = MaterialTheme.typography.body1)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Column {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { newValue ->
                            // Only allow numbers and decimal point
                            if (newValue.isEmpty() || newValue.all { char -> char.isDigit() || char == '.' }) {
                                score = newValue
                            }
                        },
                        label = { Text("Score") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !isScoreValid || (scoreValue != null && maxScoreValue != null && !isMaxScoreGreaterOrEqual)
                    )
                    if (scoreValue != null && maxScoreValue != null && !isMaxScoreGreaterOrEqual) {
                        Text(
                            "Score must be ≤ Max Score",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
                Column {
                    OutlinedTextField(
                        value = maxScore,
                        onValueChange = { newValue ->
                            // Only allow numbers and decimal point
                            if (newValue.isEmpty() || newValue.all { char -> char.isDigit() || char == '.' }) {
                                maxScore = newValue
                            }
                        },
                        label = { Text("Max Score") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !isMaxScoreValid || (scoreValue != null && maxScoreValue != null && !isMaxScoreGreaterOrEqual)
                    )
                    if (scoreValue != null && maxScoreValue != null && !isMaxScoreGreaterOrEqual) {
                        Text(
                            "Max Score must be ≥ Score",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName != null && canSubmit) {
                        onAdd(
                            subjectName!!,
                            type.trim(),
                            scoreValue!!,
                            maxScoreValue!!,
                            category.trim()
                        )
                    }
                },
                enabled = canSubmit
            ) {
                Text(if (editingGrade != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

