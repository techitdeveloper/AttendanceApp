package com.techit.attendance.data.model

data class StudentAttendanceSummary(
    val studentId: Int,
    val studentName: String,
    val rollIdentifier: String?,
    val totalDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val percentage: Float
)