package com.techit.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.StudentEntity
import com.techit.attendance.data.model.StudentAttendanceSummary
import kotlinx.coroutines.launch
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
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var summaryList by remember { mutableStateOf<List<StudentAttendanceSummary>>(emptyList()) }
    var startDate by remember { mutableStateOf(getStartOfMonth()) }
    var endDate by remember { mutableStateOf(getEndOfDay(System.currentTimeMillis())) }
    var isLoading by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadSummary()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attendance Summary - $className") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date Range Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dateFormat.format(Date(startDate)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("to", modifier = Modifier.padding(top = 8.dp))
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dateFormat.format(Date(endDate)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Divider()

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(summaryList) { summary ->
                            SummaryItem(summary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Date Pickers would need additional implementation
    // For simplicity, using basic approach here
}

@Composable
fun SummaryItem(summary: StudentAttendanceSummary) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = summary.studentName,
                style = MaterialTheme.typography.titleSmall
            )
            if (summary.rollIdentifier != null) {
                Text(
                    text = "Roll: ${summary.rollIdentifier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "P: ${summary.presentDays} | A: ${summary.absentDays} | Total: ${summary.totalDays}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "%.1f%%".format(summary.percentage),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summary.percentage >= 75) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun getStartOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun getEndOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}