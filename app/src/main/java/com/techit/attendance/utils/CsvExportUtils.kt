package com.techit.attendance.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.techit.attendance.data.model.StudentAttendanceSummary
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExportUtils {

    /**
     * Exports student attendance summary to CSV
     */
    fun exportStudentSummaryToCsv(
        context: Context,
        className: String,
        summaryList: List<StudentAttendanceSummary>,
        startDate: Long,
        endDate: Long
    ): File? {
        try {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            // Create file name
            val timestamp = timestampFormat.format(Date())
            val fileName = "Attendance_${className.replace(" ", "_")}_$timestamp.csv"

            // Create file in Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            // Write CSV content
            FileWriter(file).use { writer ->
                // Header with metadata
                writer.append("Attendance Report\n")
                writer.append("Class:,$className\n")
                writer.append("Period:,${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}\n")
                writer.append("Generated:,${dateFormat.format(Date())}\n")
                writer.append("\n")

                // Column headers
                writer.append("Student Name,Roll Number,Present Days,Absent Days,Total Days,Attendance %\n")

                // Data rows
                summaryList.forEach { summary ->
                    writer.append("${escapeCsv(summary.studentName)},")
                    writer.append("${escapeCsv(summary.rollIdentifier ?: "N/A")},")
                    writer.append("${summary.presentDays},")
                    writer.append("${summary.absentDays},")
                    writer.append("${summary.totalDays},")
                    writer.append("${"%.2f".format(summary.percentage)}\n")
                }

                // Summary statistics
                writer.append("\n")
                writer.append("Summary Statistics\n")
                val avgAttendance = if (summaryList.isNotEmpty()) {
                    summaryList.map { it.percentage }.average()
                } else 0.0
                writer.append("Total Students,${summaryList.size}\n")
                writer.append("Average Attendance,${"%.2f".format(avgAttendance)}%\n")
                writer.append("Students Above 90%,${summaryList.count { it.percentage >= 90 }}\n")
                writer.append("Students Below 75%,${summaryList.count { it.percentage < 75 }}\n")
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Exports class analytics to CSV
     */
    fun exportClassAnalyticsToCsv(
        context: Context,
        className: String,
        totalStudents: Int,
        totalDays: Int,
        averageAttendance: Float,
        bestDay: Pair<Long, Float>?,
        worstDay: Pair<Long, Float>?,
        studentsAbove90: Int,
        studentsBelow75: Int,
        startDate: Long,
        endDate: Long
    ): File? {
        try {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            val timestamp = timestampFormat.format(Date())
            val fileName = "Analytics_${className.replace(" ", "_")}_$timestamp.csv"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("Class Analytics Report\n")
                writer.append("Class:,$className\n")
                writer.append("Period:,${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}\n")
                writer.append("Generated:,${dateFormat.format(Date())}\n")
                writer.append("\n")

                writer.append("Metric,Value\n")
                writer.append("Total Students,$totalStudents\n")
                writer.append("Days Tracked,$totalDays\n")
                writer.append("Average Attendance,${"%.2f".format(averageAttendance)}%\n")
                writer.append("\n")

                if (bestDay != null && worstDay != null) {
                    writer.append("Performance Insights\n")
                    writer.append("Best Day,${dateFormat.format(Date(bestDay.first))},${"%.2f".format(bestDay.second)}%\n")
                    writer.append("Worst Day,${dateFormat.format(Date(worstDay.first))},${"%.2f".format(worstDay.second)}%\n")
                    writer.append("\n")
                }

                writer.append("Student Distribution\n")
                writer.append("Students Above 90%,$studentsAbove90\n")
                writer.append("Students Below 75%,$studentsBelow75\n")
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Opens the exported CSV file using a file picker/viewer
     */
    fun openCsvFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open CSV with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Shares the CSV file via share sheet
     */
    fun shareCsvFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share CSV via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Escapes special characters in CSV fields
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}