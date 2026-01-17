package com.techit.attendance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.utils.CsvExportUtils
import com.techit.attendance.utils.getStartOfDay
import com.techit.attendance.utils.getEndOfDay
import com.techit.attendance.utils.getStartOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ClassAnalytics(
    val totalStudents: Int,
    val totalDays: Int,
    val averageAttendance: Float,
    val bestDay: Pair<Long, Float>?,
    val worstDay: Pair<Long, Float>?,
    val studentsBelow75: Int,
    val studentsAbove90: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassAnalyticsDialog(
    database: AppDatabase,
    classId: Int,
    className: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var analytics by remember { mutableStateOf<ClassAnalytics?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Date range state
    var startDate by remember { mutableStateOf(getStartOfMonth()) }
    var endDate by remember { mutableStateOf(getEndOfDay(System.currentTimeMillis())) }

    // Quick filter state
    var selectedQuickFilter by remember { mutableStateOf<QuickFilter?>(QuickFilter.THIS_MONTH) }
    var showExportMenu by remember { mutableStateOf(false) }

    fun loadAnalytics() {
        scope.launch {
            isLoading = true

            try {
                // Get total students count
                val totalStudents = database.studentDao().getStudentCount(classId)

                if (totalStudents == 0) {
                    analytics = null
                    isLoading = false
                    return@launch
                }

                // Calculate metrics
                var totalAttendancePercentage = 0f
                var studentsBelow75 = 0
                var studentsAbove90 = 0

                // Get unique attendance dates in range
                val attendanceDates = mutableSetOf<Long>()

                // We need to manually query students since it's a Flow
                // For simplicity, let's use a different approach
                val studentIds = mutableListOf<Int>()

                // Query all students for this class directly
                for (i in 1..totalStudents) {
                    // This is a workaround - we'll query attendance and extract student IDs
                    val records = database.attendanceDao().getAttendanceByClassInRange(classId, startDate, endDate)
                    records.forEach {
                        if (!studentIds.contains(it.studentId)) {
                            studentIds.add(it.studentId)
                        }
                        attendanceDates.add(it.date)
                    }
                    break // We only need to do this once
                }

                // Calculate per-student metrics
                studentIds.forEach { studentId ->
                    val totalDays = database.attendanceDao().getTotalDaysInRange(studentId, startDate, endDate)
                    val present = database.attendanceDao().getPresentCountInRange(studentId, startDate, endDate)

                    if (totalDays > 0) {
                        val percentage = (present.toFloat() / totalDays * 100)
                        totalAttendancePercentage += percentage

                        if (percentage < 75) studentsBelow75++
                        if (percentage >= 90) studentsAbove90++
                    }
                }

                val avgAttendance = if (studentIds.isNotEmpty()) {
                    totalAttendancePercentage / studentIds.size
                } else 0f

                // Find best and worst days
                var bestDay: Pair<Long, Float>? = null
                var worstDay: Pair<Long, Float>? = null

                attendanceDates.forEach { date ->
                    val presentCount = database.attendanceDao().getPresentCountForClass(classId, date)
                    val percentage = (presentCount.toFloat() / totalStudents * 100)

                    if (bestDay == null || percentage > bestDay!!.second) {
                        bestDay = Pair(date, percentage)
                    }
                    if (worstDay == null || percentage < worstDay!!.second) {
                        worstDay = Pair(date, percentage)
                    }
                }

                analytics = ClassAnalytics(
                    totalStudents = totalStudents,
                    totalDays = attendanceDates.size,
                    averageAttendance = avgAttendance,
                    bestDay = bestDay,
                    worstDay = worstDay,
                    studentsBelow75 = studentsBelow75,
                    studentsAbove90 = studentsAbove90
                )

            } catch (e: Exception) {
                e.printStackTrace()
                analytics = null
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(startDate, endDate) {
        loadAnalytics()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Class Analytics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            className,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        // Export button
                        IconButton(
                            onClick = { showExportMenu = true },
                            enabled = analytics != null && !isExporting
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export"
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
                                    analytics?.let { data ->
                                        scope.launch {
                                            isExporting = true
                                            withContext(Dispatchers.IO) {
                                                val uri = CsvExportUtils.exportClassAnalyticsToCsv(
                                                    context = context,
                                                    className = className,
                                                    totalStudents = data.totalStudents,
                                                    totalDays = data.totalDays,
                                                    averageAttendance = data.averageAttendance,
                                                    bestDay = data.bestDay,
                                                    worstDay = data.worstDay,
                                                    studentsAbove90 = data.studentsAbove90,
                                                    studentsBelow75 = data.studentsBelow75,
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
                                    analytics?.let { data ->
                                        scope.launch {
                                            isExporting = true
                                            withContext(Dispatchers.IO) {
                                                val uri = CsvExportUtils.exportClassAnalyticsToCsv(
                                                    context = context,
                                                    className = className,
                                                    totalStudents = data.totalStudents,
                                                    totalDays = data.totalDays,
                                                    averageAttendance = data.averageAttendance,
                                                    bestDay = data.bestDay,
                                                    worstDay = data.worstDay,
                                                    studentsAbove90 = data.studentsAbove90,
                                                    studentsBelow75 = data.studentsBelow75,
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

                // Quick filters
                QuickFilterRow(
                    selectedFilter = selectedQuickFilter,
                    onFilterSelected = { filter ->
                        selectedQuickFilter = filter
                        val (start, end) = filter.getDateRange()
                        startDate = start
                        endDate = end
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date range display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

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
                                "Loading analytics...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (analytics == null || analytics!!.totalDays == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No attendance data for this period",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AnalyticsContent(
                        analytics = analytics!!,
                        dateFormat = dateFormat,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun QuickFilterRow(
    selectedFilter: QuickFilter?,
    onFilterSelected: (QuickFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        filter.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
fun AnalyticsContent(
    analytics: ClassAnalytics,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview metrics
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
                Text(
                    "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricItem("Students", "${analytics.totalStudents}")
                    MetricItem("Days", "${analytics.totalDays}")
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Average Attendance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "%.1f%%".format(analytics.averageAttendance),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Best and worst days
        if (analytics.bestDay != null && analytics.worstDay != null && analytics.totalDays > 1) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Performance Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Best day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Best Day",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    dateFormat.format(Date(analytics.bestDay.first)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            "%.1f%%".format(analytics.bestDay.second),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider()

                    // Worst day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Worst Day",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateFormat.format(Date(analytics.worstDay.first)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Text(
                            "%.1f%%".format(analytics.worstDay.second),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Student distribution
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Student Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricItem(
                        "Above 90%",
                        "${analytics.studentsAbove90}",
                        MaterialTheme.colorScheme.primary
                    )
                    MetricItem(
                        "Below 75%",
                        "${analytics.studentsBelow75}",
                        MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface  // CHANGED from valueColor parameter
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface  // CHANGED from onSurfaceVariant
        )
    }
}

enum class QuickFilter(val label: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month");

    fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = getEndOfDay(System.currentTimeMillis())

        val startDate = when (this) {
            LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                getStartOfDay(calendar.timeInMillis)
            }
            LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                getStartOfDay(calendar.timeInMillis)
            }
            THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                getStartOfDay(calendar.timeInMillis)
            }
            LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = getStartOfDay(calendar.timeInMillis)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                return Pair(start, getEndOfDay(calendar.timeInMillis))
            }
        }

        return Pair(startDate, endDate)
    }
}