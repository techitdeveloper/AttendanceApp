package com.techit.attendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techit.attendance.ads.AdManager
import com.techit.attendance.ads.BannerAdView
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.StudentEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    database: AppDatabase,
    classId: Int,
    adManager: AdManager,
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
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(classId) {
        className = database.classDao().getClassById(classId)?.name ?: "Students"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(className)
                        Text(
                            text = "${students.size} ${if (students.size == 1) "student" else "students"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (students.isNotEmpty()) {
                        IconButton(onClick = { showSummary = true }) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Summary",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (students.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mark Attendance FAB
                    FloatingActionButton(
                        onClick = onMarkAttendance,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Mark Attendance")
                    }

                    // Add Student FAB
                    SmallFloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Student",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Student")
                }
            }
        },
        bottomBar = {
            Column {
                // Action bar when students exist
                if (students.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onMarkAttendance,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Mark Attendance")
                            }
                        }
                    }
                }
                BannerAdView(adManager = adManager)
            }
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            // Enhanced Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Animated icon
                    val infiniteTransition = rememberInfiniteTransition(label = "scale")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )

                    Text(
                        text = "No Students Yet",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Add students to $className to start tracking their attendance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Your First Student", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    EnhancedStudentCard(
                        student = student,
                        database = database,
                        onDelete = {
                            scope.launch {
                                database.studentDao().deleteStudent(student)
                                snackbarMessage = "${student.name} removed"
                                showSnackbar = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Student Dialog - Enhanced
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                studentName = ""
                rollNumber = ""
            },
            icon = {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Add Student") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter student details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name *") },
                        placeholder = { Text("e.g., John Doe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll Number (Optional)") },
                        placeholder = { Text("e.g., 101, A-25") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null)
                        }
                    )
                }
            },
            confirmButton = {
                Button(
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
                                snackbarMessage = "${studentName.trim()} added to $className"
                                showSnackbar = true
                                studentName = ""
                                rollNumber = ""
                                showDialog = false
                            }
                        }
                    },
                    enabled = studentName.isNotBlank()
                ) {
                    Text("Add Student")
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

    // Summary Dialog
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
fun EnhancedStudentCard(
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

    // Determine card color based on attendance
    val containerColor = MaterialTheme.colorScheme.surface  // Always use surface

    val borderColor = when {
        presentCount + absentCount == 0 -> Color.Transparent
        percentage >= 90 -> MaterialTheme.colorScheme.primary
        percentage >= 75 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (borderColor != Color.Transparent) {  // ADD THIS
            BorderStroke(2.dp, borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Student Avatar with Initials
                val initials = student.name
                    .split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .ifEmpty { "?" }

                val avatarColor = getColorForInitial(initials.firstOrNull() ?: '?')

                Surface(
                    shape = CircleShape,
                    color = avatarColor,
                    modifier = Modifier.size(56.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            // CHANGED: Use contentColorFor to get proper contrast
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (student.rollIdentifier != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Roll: ${student.rollIdentifier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Attendance Stats
                    if (presentCount + absentCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AttendanceBadge(
                                icon = Icons.Default.CheckCircle,
                                count = presentCount,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AttendanceBadge(
                                icon = Icons.Default.Cancel,
                                count = absentCount,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Percentage bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Attendance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface  // CHANGED from onSurfaceVariant
                                )
                                Text(
                                    text = "%.1f%%".format(percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        percentage >= 90 -> MaterialTheme.colorScheme.primary
                                        percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }

                            LinearProgressIndicator(
                                progress = { percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = when {
                                    percentage >= 90 -> MaterialTheme.colorScheme.primary
                                    percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "No attendance records yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Student",
                    tint = MaterialTheme.colorScheme.error  // This should already be correct
                )
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Student?") },
            text = {
                Text("Are you sure you want to remove ${student.name}? This will delete all attendance records for this student. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
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

@Composable
fun AttendanceBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = color,  // Keep the color for badges
            fontWeight = FontWeight.Bold
        )
    }
}

// Generate consistent colors for student avatars
@Composable
fun getColorForInitial(char: Char): Color {
    // Use theme-aware colors instead of hardcoded ones
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        Color(0xFF00897B), // Teal - works in both themes
        Color(0xFFE64A19), // Deep Orange - works in both themes
        Color(0xFF5E35B1), // Deep Purple - works in both themes
        Color(0xFFD84315), // Brown - works in both themes
    )
    val index = (char.code % colors.size)
    return colors[index]
}