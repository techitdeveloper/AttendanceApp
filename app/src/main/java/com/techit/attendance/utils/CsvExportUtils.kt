package com.techit.attendance.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.techit.attendance.data.model.StudentAttendanceSummary
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExportUtils {

    /**
     * Exports student attendance summary to CSV using MediaStore (Android 10+)
     */
    fun exportStudentSummaryToCsv(
        context: Context,
        className: String,
        summaryList: List<StudentAttendanceSummary>,
        startDate: Long,
        endDate: Long
    ): Uri? {
        return try {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            val timestamp = timestampFormat.format(Date())
            val fileName = "Attendance_${className.replace(" ", "_")}_$timestamp.csv"

            // Create CSV content
            val csvContent = buildString {
                appendLine("Attendance Report")
                appendLine("Class:,$className")
                appendLine("Period:,${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}")
                appendLine("Generated:,${dateFormat.format(Date())}")
                appendLine()
                appendLine("Student Name,Roll Number,Present Days,Absent Days,Total Days,Attendance %")

                summaryList.forEach { summary ->
                    appendLine("${escapeCsv(summary.studentName)},${escapeCsv(summary.rollIdentifier ?: "N/A")},${summary.presentDays},${summary.absentDays},${summary.totalDays},${"%.2f".format(summary.percentage)}")
                }

                appendLine()
                appendLine("Summary Statistics")
                val avgAttendance = if (summaryList.isNotEmpty()) {
                    summaryList.map { it.percentage }.average()
                } else 0.0
                appendLine("Total Students,${summaryList.size}")
                appendLine("Average Attendance,${"%.2f".format(avgAttendance)}%")
                appendLine("Students Above 90%,${summaryList.count { it.percentage >= 90 }}")
                appendLine("Students Below 75%,${summaryList.count { it.percentage < 75 }}")
            }

            // Save using MediaStore
            saveCsvToDownloads(context, fileName, csvContent)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports class analytics to CSV using MediaStore
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
    ): Uri? {
        return try {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            val timestamp = timestampFormat.format(Date())
            val fileName = "Analytics_${className.replace(" ", "_")}_$timestamp.csv"

            val csvContent = buildString {
                appendLine("Class Analytics Report")
                appendLine("Class:,$className")
                appendLine("Period:,${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}")
                appendLine("Generated:,${dateFormat.format(Date())}")
                appendLine()
                appendLine("Metric,Value")
                appendLine("Total Students,$totalStudents")
                appendLine("Days Tracked,$totalDays")
                appendLine("Average Attendance,${"%.2f".format(averageAttendance)}%")
                appendLine()

                if (bestDay != null && worstDay != null) {
                    appendLine("Performance Insights")
                    appendLine("Best Day,${dateFormat.format(Date(bestDay.first))},${"%.2f".format(bestDay.second)}%")
                    appendLine("Worst Day,${dateFormat.format(Date(worstDay.first))},${"%.2f".format(worstDay.second)}%")
                    appendLine()
                }

                appendLine("Student Distribution")
                appendLine("Students Above 90%,$studentsAbove90")
                appendLine("Students Below 75%,$studentsBelow75")
            }

            saveCsvToDownloads(context, fileName, csvContent)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save CSV to Downloads folder using MediaStore (Android 10+)
     * No permissions needed!
     */
    private fun saveCsvToDownloads(context: Context, fileName: String, content: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
        }

        return uri
    }

    /**
     * Opens the exported CSV file using a file picker/viewer
     */
    fun openCsvFile(context: Context, uri: Uri) {
        try {
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
    fun shareCsvFile(context: Context, uri: Uri) {
        try {
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