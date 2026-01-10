package com.techit.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.StudentEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    database: AppDatabase,
    classId: Int,
    onMarkAttendance: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val students by database.studentDao().getStudentsByClass(classId).collectAsState(initial = emptyList())
    var className by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var studentName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var showSummary by remember { mutableStateOf(false) }

    LaunchedEffect(classId) {
        className = database.classDao().getClassById(classId)?.name ?: "Students"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(className) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (students.isNotEmpty()) {
                        IconButton(onClick = { showSummary = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Summary")
                        }
                        Button(
                            onClick = onMarkAttendance,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Mark Attendance")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students yet.\nTap + to add a student.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    StudentItem(
                        student = student,
                        database = database,
                        onDelete = {
                            scope.launch {
                                database.studentDao().deleteStudent(student)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                studentName = ""
                rollNumber = ""
            },
            title = { Text("Add Student") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll Number (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (studentName.isNotBlank()) {
                            scope.launch {
                                database.studentDao().insertStudent(
                                    StudentEntity(
                                        classId = classId,
                                        name = studentName.trim(),
                                        rollIdentifier = rollNumber.trim().ifBlank { null }
                                    )
                                )
                                studentName = ""
                                rollNumber = ""
                                showDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        studentName = ""
                        rollNumber = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSummary) {
        AttendanceSummaryDialog(
            database = database,
            classId = classId,
            className = className,
            students = students,
            onDismiss = { showSummary = false }
        )
    }
}

@Composable
fun StudentItem(
    student: StudentEntity,
    database: AppDatabase,
    onDelete: () -> Unit
) {
    var presentCount by remember { mutableStateOf(0) }
    var absentCount by remember { mutableStateOf(0) }
    var percentage by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(student.id) {
        presentCount = database.attendanceDao().getPresentCount(student.id)
        absentCount = database.attendanceDao().getAbsentCount(student.id)
        val total = presentCount + absentCount
        percentage = if (total > 0) (presentCount.toFloat() / total * 100) else 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (student.rollIdentifier != null) {
                    Text(
                        text = "Roll: ${student.rollIdentifier}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Present: $presentCount | Absent: $absentCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (presentCount + absentCount > 0) {
                    Text(
                        text = "Attendance: %.1f%%".format(percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (percentage >= 75) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Student",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${student.name}? This will also delete all attendance records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}