package com.techit.attendance.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.techit.attendance.ads.AdManager
import com.techit.attendance.ads.ConsentManager
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.ui.screens.AttendanceScreen
import com.techit.attendance.ui.screens.ClassListScreen
import com.techit.attendance.ui.screens.StudentListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    database: AppDatabase,
    adManager: AdManager,
    consentManager: ConsentManager,
    activity: Activity
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ClassList.route
    ) {
        composable(Screen.ClassList.route) {
            ClassListScreen(
                database = database,
                adManager = adManager,
                consentManager = consentManager,
                activity = activity,
                onClassClick = { classId ->
                    navController.navigate(Screen.StudentList.createRoute(classId))
                }
            )
        }

        composable(
            route = Screen.StudentList.route,
            arguments = listOf(navArgument("classId") { type = NavType.IntType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getInt("classId") ?: return@composable
            StudentListScreen(
                database = database,
                classId = classId,
                onMarkAttendance = {
                    navController.navigate(Screen.Attendance.createRoute(classId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Attendance.route,
            arguments = listOf(navArgument("classId") { type = NavType.IntType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getInt("classId") ?: return@composable
            AttendanceScreen(
                database = database,
                classId = classId,
                adManager = adManager,
                activity = activity,
                onBack = { navController.popBackStack() }
            )
        }
    }
}