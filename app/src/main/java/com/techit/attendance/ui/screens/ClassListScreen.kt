package com.techit.attendance.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

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
                title = {
                    Column {
                        Text("My Classes")
                        Text(
                            text = "${classes.size} ${if (classes.size == 1) "class" else "classes"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
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
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Security, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
            }
        },
        bottomBar = {
            BannerAdView(adManager = adManager)
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
        if (classes.isEmpty()) {
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
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )

                    Text(
                        text = "No Classes Yet",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Create your first class to start tracking student attendance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Your First Class", style = MaterialTheme.typography.titleMedium)
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
                items(classStatusList, key = { it.classId }) { classStatus ->
                    EnhancedClassCard(
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
                                    snackbarMessage = "${classStatus.className} deleted"
                                    showSnackbar = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Class Dialog - Enhanced
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                className = ""
            },
            icon = {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Create New Class") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a name for your class",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class Name") },
                        placeholder = { Text("e.g., Math 101, Grade 10-A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (className.isNotBlank()) {
                            scope.launch {
                                database.classDao().insertClass(
                                    ClassEntity(name = className.trim())
                                )
                                snackbarMessage = "Class '${className.trim()}' created!"
                                showSnackbar = true
                                className = ""
                                showDialog = false
                            }
                        }
                    },
                    enabled = className.isNotBlank()
                ) {
                    Text("Create")
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

    // Analytics Dialog
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
fun EnhancedClassCard(
    classStatus: ClassWithStatus,
    onClick: () -> Unit,
    onAnalytics: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Determine card color based on status
    val containerColor = when {
        classStatus.totalStudents == 0 -> MaterialTheme.colorScheme.surfaceVariant
        classStatus.attendanceTaken -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Class initial in circle
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = classStatus.className.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Text(
                            text = classStatus.className,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${classStatus.totalStudents} ${if (classStatus.totalStudents == 1) "student" else "students"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { showOptionsMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
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

            Spacer(modifier = Modifier.height(12.dp))

            // Status section
            if (classStatus.totalStudents == 0) {
                StatusRow(
                    icon = Icons.Default.Info,
                    text = "Add students to get started",
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (classStatus.attendanceTaken) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRow(
                        icon = Icons.Default.CheckCircle,
                        text = "Attendance marked today",
                        iconTint = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Calculate percentage based on total students, not just those marked
                        val percentage = if (classStatus.totalStudents > 0) {
                            (classStatus.todayPresent.toFloat() / classStatus.totalStudents * 100)
                        } else 0f

                        AttendanceStatChip(
                            label = "Present",
                            value = "${classStatus.todayPresent}",
                            color = MaterialTheme.colorScheme.primary
                        )
                        AttendanceStatChip(
                            label = "Absent",
                            value = "${classStatus.todayAbsent}",
                            color = MaterialTheme.colorScheme.error
                        )
                        AttendanceStatChip(
                            label = "Rate",
                            value = "%.0f%%".format(percentage),
                            color = when {
                                percentage >= 90 -> MaterialTheme.colorScheme.primary
                                percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            } else {
                StatusRow(
                    icon = Icons.Default.PendingActions,
                    text = "Attendance pending for today",
                    iconTint = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.onErrorContainer
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
            title = { Text("Delete Class?") },
            text = {
                Text("This will permanently delete '${classStatus.className}' with all ${classStatus.totalStudents} students and their attendance records. This action cannot be undone.")
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
fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AttendanceStatChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}