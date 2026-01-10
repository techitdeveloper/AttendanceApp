package com.techit.attendance.data.model

data class ClassWithStatus(
    val classId: Int,
    val className: String,
    val totalStudents: Int,
    val todayPresent: Int,
    val todayAbsent: Int,
    val attendanceTaken: Boolean
)