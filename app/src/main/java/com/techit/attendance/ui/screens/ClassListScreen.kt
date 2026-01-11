package com.techit.attendance.ui.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    // Analytics dialog state
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var selectedClassForAnalytics by remember { mutableStateOf<Pair<Int, String>?>(null) }

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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "No classes yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to add your first class",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                items(classStatusList) { classStatus ->
                    ClassItemWithStatus(
                        classStatus = classStatus,
                        onClick = { onClassClick(classStatus.classId) },
                        onAnalytics = {
                            selectedClassForAnalytics = Pair(classStatus.classId, classStatus.className)
                            showAnalyticsDialog = true
                        },
                        onDelete = {
                            scope.launch {
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
                    placeholder = { Text("e.g., Math 101, Physics A") },
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
                    },
                    enabled = className.isNotBlank()
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

    // Show analytics dialog
    if (showAnalyticsDialog && selectedClassForAnalytics != null) {
        ClassAnalyticsDialog(
            database = database,
            classId = selectedClassForAnalytics!!.first,
            className = selectedClassForAnalytics!!.second,
            onDismiss = {
                showAnalyticsDialog = false
                selectedClassForAnalytics = null
            }
        )
    }
}

@Composable
fun ClassItemWithStatus(
    classStatus: ClassWithStatus,
    onClick: () -> Unit,
    onAnalytics: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with class name and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = classStatus.className,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showOptionsMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Analytics") },
                        onClick = {
                            showOptionsMenu = false
                            onAnalytics()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Class") },
                        onClick = {
                            showOptionsMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attendance status
            if (classStatus.totalStudents == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No students added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (classStatus.attendanceTaken) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Today: ${classStatus.todayPresent}/${classStatus.totalStudents} present",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val percentage = if (classStatus.totalStudents > 0) {
                    (classStatus.todayPresent.toFloat() / classStatus.totalStudents * 100)
                } else 0f

                Text(
                    text = "%.1f%% attendance".format(percentage),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (percentage >= 75) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Today's attendance pending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "${classStatus.totalStudents} students waiting",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Class?") },
            text = {
                Text("This will permanently delete ${classStatus.className} with all ${classStatus.totalStudents} students and their attendance records. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
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