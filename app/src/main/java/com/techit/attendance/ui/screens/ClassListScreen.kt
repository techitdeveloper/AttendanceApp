package com.techit.attendance.ui.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techit.attendance.ads.AdManager
import com.techit.attendance.ads.BannerAdView
import com.techit.attendance.ads.ConsentManager
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.data.entity.ClassEntity
import com.techit.attendance.data.model.ClassWithStatus
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassListScreen(
    database: AppDatabase,
    adManager: AdManager,
    consentManager: ConsentManager,
    activity: Activity,
    onClassClick: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val classes by database.classDao().getAllClasses().collectAsState(initial = emptyList())
    var classStatusList by remember { mutableStateOf<List<ClassWithStatus>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var className by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val today = remember {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

    // Load attendance status for all classes
    LaunchedEffect(classes) {
        classStatusList = classes.map { classEntity ->
            val totalStudents = database.studentDao().getStudentCount(classEntity.id)
            val attendanceCount = database.attendanceDao().getAttendanceCountForClass(classEntity.id, today)
            val presentCount = database.attendanceDao().getPresentCountForClass(classEntity.id, today)
            val absentCount = database.attendanceDao().getAbsentCountForClass(classEntity.id, today)

            ClassWithStatus(
                classId = classEntity.id,
                className = classEntity.name,
                totalStudents = totalStudents,
                todayPresent = presentCount,
                todayAbsent = absentCount,
                attendanceTaken = attendanceCount > 0
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Classes") },
                actions = {
                    // Show privacy options if required
                    if (consentManager.isPrivacyOptionsRequired()) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Privacy Settings") },
                                onClick = {
                                    showMenu = false
                                    consentManager.showPrivacyOptionsForm(activity) { canRequestAds ->
                                        // User changed consent settings
                                        if (canRequestAds) {
                                            adManager.enableAds()
                                            adManager.loadInterstitialAd()
                                        } else {
                                            adManager.disableAds()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
            }
        },
        bottomBar = {
            // Banner Ad at bottom
            BannerAdView(adManager = adManager)
        }
    ) { padding ->
        if (classes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No classes yet.\nTap + to add a class.",
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
                items(classStatusList) { classStatus ->
                    ClassItemWithStatus(
                        classStatus = classStatus,
                        onClick = { onClassClick(classStatus.classId) },
                        onDelete = {
                            scope.launch {
                                // Find the actual ClassEntity to delete
                                val classEntity = classes.find { it.id == classStatus.classId }
                                classEntity?.let {
                                    database.classDao().deleteClass(it)
                                }
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
                className = ""
            },
            title = { Text("Add Class") },
            text = {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (className.isNotBlank()) {
                            scope.launch {
                                database.classDao().insertClass(
                                    ClassEntity(name = className.trim())
                                )
                                className = ""
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
                        className = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ClassItemWithStatus(
    classStatus: ClassWithStatus,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = classStatus.className,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Show attendance status
                if (classStatus.totalStudents == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "No students added",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (classStatus.attendanceTaken) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Today: ${classStatus.todayPresent}/${classStatus.totalStudents} present",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Show percentage
                    val percentage = if (classStatus.totalStudents > 0) {
                        (classStatus.todayPresent.toFloat() / classStatus.totalStudents * 100)
                    } else 0f

                    Text(
                        text = "Attendance: %.1f%%".format(percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (percentage >= 75) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Today's attendance pending (${classStatus.totalStudents} students)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Class",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Class") },
            text = {
                Text("Are you sure you want to delete ${classStatus.className}? This will delete all ${classStatus.totalStudents} students and their attendance records.")
            },
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