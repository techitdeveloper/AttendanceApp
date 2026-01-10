package com.techit.attendance.ui.navigation

sealed class Screen(val route: String) {
    object ClassList : Screen("class_list")
    object StudentList : Screen("student_list/{classId}") {
        fun createRoute(classId: Int) = "student_list/$classId"
    }
    object Attendance : Screen("attendance/{classId}") {
        fun createRoute(classId: Int) = "attendance/$classId"
    }
}