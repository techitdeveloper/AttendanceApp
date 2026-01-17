package com.techit.attendance.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techit.attendance.ads.AdManager
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.AttendanceEntity
import com.techit.attendance.data.entity.StudentEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.techit.attendance.utils.getStartOfDay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    database: AppDatabase,
    classId: Int,
    adManager: AdManager,
    activity: Activity,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val students by database.studentDao().getStudentsByClass(classId).collectAsState(initial = emptyList())

    val attendanceMap = remember { mutableStateMapOf<Int, Boolean>() }
    var selectedDate by remember { mutableStateOf(getStartOfDay(System.currentTimeMillis())) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var isSaving by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var className by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var attendanceExists by remember { mutableStateOf(false) }

    LaunchedEffect(classId) {
        className = database.classDao().getClassById(classId)?.name ?: "Attendance"
    }

    LaunchedEffect(students, selectedDate) {
        attendanceMap.clear()
        val existingRecords = database.attendanceDao().getAttendanceByClassAndDate(classId, selectedDate)
        attendanceExists = existingRecords.isNotEmpty()

        students.forEach { student ->
            val existing = existingRecords.find { it.studentId == student.id }
            attendanceMap[student.id] = existing?.isPresent ?: false
        }
    }

    val presentCount = attendanceMap.values.count { it }
    val totalCount = students.size
    val absentCount = totalCount - presentCount
    val percentage = if (totalCount > 0) (presentCount.toFloat() / totalCount * 100) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(className)
                        Text(
                            text = dateFormat.format(Date(selectedDate)),
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
                    // Quick actions
                    if (totalCount > 0) {
                        IconButton(
                            onClick = {
                                // Mark all present
                                students.forEach { student ->
                                    attendanceMap[student.id] = true
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Mark All Present",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                // Mark all absent
                                students.forEach { student ->
                                    attendanceMap[student.id] = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.RemoveDone,
                                contentDescription = "Mark All Absent",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Save button bar
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    students.forEach { student ->
                                        database.attendanceDao().insertAttendance(
                                            AttendanceEntity(
                                                studentId = student.id,
                                                date = selectedDate,
                                                isPresent = attendanceMap[student.id] ?: false
                                            )
                                        )
                                    }
                                    isSaving = false
                                    showSuccessMessage = true

                                    // Show interstitial ad after successful save
                                    adManager.showInterstitialAd(activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Saving...")
                            } else {
                                Icon(
                                    if (attendanceExists) Icons.Default.Update else Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (attendanceExists) "Update Attendance" else "Save Attendance",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = { showDatePicker = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (attendanceExists) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "Edit Mode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Change Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Summary Card
            if (totalCount > 0) {
                val summaryBorderColor = when {
                    percentage >= 90 -> MaterialTheme.colorScheme.primary
                    percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                    percentage > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface  // CHANGED - always surface
                    ),
                    border = BorderStroke(2.dp, summaryBorderColor)  // ADD THIS
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attendance Summary",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = CircleShape,
                                color = when {
                                    percentage >= 90 -> MaterialTheme.colorScheme.primary
                                    percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                                    percentage > 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            ) {
                                Text(
                                    text = "%.0f%%".format(percentage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    // CHANGED: Use proper on-color
                                    color = when {
                                        percentage >= 90 -> MaterialTheme.colorScheme.onPrimary
                                        percentage >= 75 -> MaterialTheme.colorScheme.onTertiary
                                        percentage > 0 -> MaterialTheme.colorScheme.onError
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryStatColumn(
                                icon = Icons.Default.CheckCircle,
                                label = "Present",
                                value = "$presentCount",
                                color = MaterialTheme.colorScheme.primary
                            )

                            Divider(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(1.dp)
                            )

                            SummaryStatColumn(
                                icon = Icons.Default.Cancel,
                                label = "Absent",
                                value = "$absentCount",
                                color = MaterialTheme.colorScheme.error
                            )

                            Divider(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(1.dp)
                            )

                            SummaryStatColumn(
                                icon = Icons.Default.Group,
                                label = "Total",
                                value = "$totalCount",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Student List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    EnhancedAttendanceItem(
                        student = student,
                        isPresent = attendanceMap[student.id] ?: false,
                        onCheckedChange = { checked ->
                            attendanceMap[student.id] = checked
                        }
                    )
                }
            }
        }
    }

    // Modern Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = getStartOfDay(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    // Success Message
    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
            onBack()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Attendance saved successfully!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedAttendanceItem(
    student: StudentEntity,
    isPresent: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Student Avatar
    val initials = student.name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    val avatarColor = getAttendanceColorForInitial(initials.firstOrNull() ?: '?')

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isPresent)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = avatarColor,
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(
                        2.dp,
                        if (isPresent) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            // CHANGED: Use surface color for better contrast
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPresent) FontWeight.SemiBold else FontWeight.Normal
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
                                text = student.rollIdentifier,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Status indicator with checkbox
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPresent) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Present",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Absent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Checkbox(
                    checked = isPresent,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun SummaryStatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun getAttendanceColorForInitial(char: Char): Color {
    // Use theme-aware colors
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        Color(0xFF00897B),
        Color(0xFFE64A19),
        Color(0xFF5E35B1),
        Color(0xFFD84315),
    )
    return colors[(char.code % colors.size)]
}