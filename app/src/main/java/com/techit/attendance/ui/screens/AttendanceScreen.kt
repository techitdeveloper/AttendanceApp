package com.techit.attendance.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techit.attendance.ads.AdManager
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.AttendanceEntity
import com.techit.attendance.data.entity.StudentEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(className) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (attendanceExists) "Update Attendance" else "Save Attendance")
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
                    .padding(16.dp),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(selectedDate)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (attendanceExists) {
                            Text(
                                text = "Attendance already marked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Summary Card
            val presentCount = attendanceMap.values.count { it }
            val totalCount = students.size

            if (totalCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$presentCount",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Present",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${totalCount - presentCount}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Absent",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalCount",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    AttendanceItem(
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

    if (showDatePicker) {
        DatePickerDialog(
            currentDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            showSuccessMessage = false
            onBack()
        }

        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Attendance saved successfully!")
        }
    }
}

@Composable
fun AttendanceItem(
    student: StudentEntity,
    isPresent: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
            }
            Checkbox(
                checked = isPresent,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    currentDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = currentDate

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val today = Calendar.getInstance()
    val maxYear = today.get(Calendar.YEAR)
    val minYear = maxYear - 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Year Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Year:", modifier = Modifier.width(60.dp))
                    var yearExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedYear.toString(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            (minYear..maxYear).reversed().forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectedYear = year
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Month Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Month:", modifier = Modifier.width(60.dp))
                    var monthExpanded by remember { mutableStateOf(false) }
                    val months = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = months[selectedMonth],
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEachIndexed { index, month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        selectedMonth = index
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Day Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Day:", modifier = Modifier.width(60.dp))
                    var dayExpanded by remember { mutableStateOf(false) }
                    val maxDays = calendar.apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, selectedMonth)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)

                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDay.toString(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            (1..maxDays).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(day.toString()) },
                                    onClick = {
                                        selectedDay = day
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newCalendar = Calendar.getInstance()
                    newCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    newCalendar.set(Calendar.MILLISECOND, 0)

                    // Only allow past or today's date
                    if (newCalendar.timeInMillis <= today.timeInMillis) {
                        onDateSelected(newCalendar.timeInMillis)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
fun getStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}