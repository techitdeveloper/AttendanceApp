package com.techit.attendance.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.StudentEntity
import com.techit.attendance.data.model.StudentAttendanceSummary
import com.techit.attendance.utils.CsvExportUtils
import com.techit.attendance.utils.getEndOfDay
import com.techit.attendance.utils.getStartOfDay
import com.techit.attendance.utils.getStartOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSummaryDialog(
    database: AppDatabase,
    classId: Int,
    className: String,
    students: List<StudentEntity>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var summaryList by remember { mutableStateOf<List<StudentAttendanceSummary>>(emptyList()) }
    var startDate by remember { mutableStateOf(getStartOfMonth()) }
    var endDate by remember { mutableStateOf(getEndOfDay(System.currentTimeMillis())) }
    var isLoading by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var filterOption by remember { mutableStateOf<FilterOption?>(null) }

    fun loadSummary() {
        scope.launch {
            isLoading = true
            summaryList = students.map { student ->
                val total = database.attendanceDao().getTotalDaysInRange(student.id, startDate, endDate)
                val present = database.attendanceDao().getPresentCountInRange(student.id, startDate, endDate)
                val absent = total - present
                val percentage = if (total > 0) (present.toFloat() / total * 100) else 0f

                StudentAttendanceSummary(
                    studentId = student.id,
                    studentName = student.name,
                    rollIdentifier = student.rollIdentifier,
                    totalDays = total,
                    presentDays = present,
                    absentDays = absent,
                    percentage = percentage
                )
            }.let { list ->
                // Apply filter
                when (filterOption) {
                    FilterOption.EXCELLENT -> list.filter { it.percentage >= 90 }
                    FilterOption.GOOD -> list.filter { it.percentage >= 75 && it.percentage < 90 }
                    FilterOption.LOW -> list.filter { it.percentage < 75 }
                    null -> list
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(startDate, endDate, filterOption) {
        loadSummary()
    }

    // Calculate overall stats
    val avgAttendance = if (summaryList.isNotEmpty()) {
        summaryList.map { it.percentage }.average().toFloat()
    } else 0f
    val excellentCount = summaryList.count { it.percentage >= 90 }
    val goodCount = summaryList.count { it.percentage >= 75 && it.percentage < 90 }
    val lowCount = summaryList.count { it.percentage < 75 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with gradient background
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        "Attendance Summary",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    className,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { showExportMenu = true },
                                    enabled = summaryList.isNotEmpty() && !isExporting
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export CSV") },
                                        onClick = {
                                            showExportMenu = false
                                            scope.launch {
                                                isExporting = true
                                                withContext(Dispatchers.IO) {
                                                    val uri = CsvExportUtils.exportStudentSummaryToCsv(
                                                        context = context,
                                                        className = className,
                                                        summaryList = summaryList,
                                                        startDate = startDate,
                                                        endDate = endDate
                                                    )

                                                    withContext(Dispatchers.Main) {
                                                        if (uri != null) {
                                                            Toast.makeText(
                                                                context,
                                                                "Exported to Downloads",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Export failed",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                        isExporting = false
                                                    }
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.FileDownload, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share CSV") },
                                        onClick = {
                                            showExportMenu = false
                                            scope.launch {
                                                isExporting = true
                                                withContext(Dispatchers.IO) {
                                                    val uri = CsvExportUtils.exportStudentSummaryToCsv(
                                                        context = context,
                                                        className = className,
                                                        summaryList = summaryList,
                                                        startDate = startDate,
                                                        endDate = endDate
                                                    )

                                                    withContext(Dispatchers.Main) {
                                                        if (uri != null) {
                                                            CsvExportUtils.shareCsvFile(context, uri)
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Export failed",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                        isExporting = false
                                                    }
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                        }
                                    )
                                }

                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Overall Stats Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MiniStatCard(
                                value = "${summaryList.size}",
                                label = "Students",
                                icon = Icons.Default.Group,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCard(
                                value = "%.0f%%".format(avgAttendance),
                                label = "Avg Rate",
                                icon = Icons.Default.TrendingUp,
                                color = when {
                                    avgAttendance >= 90 -> MaterialTheme.colorScheme.primary
                                    avgAttendance >= 75 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Date Range & Filters
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date Range Selector
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Date Range",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "From",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = dateFormat.format(Date(startDate)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .size(20.dp)
                                )

                                OutlinedButton(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "To",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = dateFormat.format(Date(endDate)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),  // Add this
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterOption == null,
                            onClick = { filterOption = null },
                            label = { Text("All (${ students.size})") },
                            leadingIcon = if (filterOption == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = filterOption == FilterOption.EXCELLENT,
                            onClick = { filterOption = FilterOption.EXCELLENT },
                            label = { Text("≥90% ($excellentCount)") },
                            leadingIcon = if (filterOption == FilterOption.EXCELLENT) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        FilterChip(
                            selected = filterOption == FilterOption.GOOD,
                            onClick = { filterOption = FilterOption.GOOD },
                            label = { Text("75-89% ($goodCount)") },
                            leadingIcon = if (filterOption == FilterOption.GOOD) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                        FilterChip(
                            selected = filterOption == FilterOption.LOW,
                            onClick = { filterOption = FilterOption.LOW },
                            label = { Text("<75% ($lowCount)") },
                            leadingIcon = if (filterOption == FilterOption.LOW) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }

                HorizontalDivider()

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Loading summary...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (summaryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                if (filterOption != null) "No students match this filter" else "No attendance data",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(summaryList, key = { it.studentId }) { summary ->
                            EnhancedSummaryItem(summary)
                        }
                    }
                }

                // Bottom Action
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                    ) {
                        Text("Close", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        if (it <= endDate) {
                            startDate = getStartOfDay(it)
                        }
                    }
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        if (it >= startDate) {
                            endDate = getEndOfDay(it)
                        }
                    }
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun EnhancedSummaryItem(summary: StudentAttendanceSummary) {
    val initials = summary.studentName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    val avatarColor = getSummaryColorForInitial(initials.firstOrNull() ?: '?')

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                summary.percentage >= 90 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                summary.percentage >= 75 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        ),
        border = BorderStroke(
            2.dp,
            when {
                summary.percentage >= 90 -> MaterialTheme.colorScheme.primary
                summary.percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = avatarColor,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary.rollIdentifier != null) {
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
                            text = summary.rollIdentifier,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SmallStatItem(
                        icon = Icons.Default.CheckCircle,
                        value = "${summary.presentDays}",
                        label = "Present",
                        color = MaterialTheme.colorScheme.primary
                    )
                    SmallStatItem(
                        icon = Icons.Default.Cancel,
                        value = "${summary.absentDays}",
                        label = "Absent",
                        color = MaterialTheme.colorScheme.error
                    )
                    SmallStatItem(
                        icon = Icons.Default.CalendarMonth,
                        value = "${summary.totalDays}",
                        label = "Days",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Percentage badge
            Surface(
                shape = CircleShape,
                color = when {
                    summary.percentage >= 90 -> MaterialTheme.colorScheme.primary
                    summary.percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "%.0f%%".format(summary.percentage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MiniStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SmallStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun getSummaryColorForInitial(char: Char): Color {
    val colors = listOf(
        Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F), Color(0xFFF57C00),
        Color(0xFF7B1FA2), Color(0xFF0097A7), Color(0xFFC2185B), Color(0xFF5D4037),
    )
    return colors[(char.code % colors.size)]
}

enum class FilterOption {
    EXCELLENT,  // >= 90%
    GOOD,       // 75-89%
    LOW         // < 75%
}