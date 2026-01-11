package com.techit.attendance.data.dao

import androidx.room.*
import com.techit.attendance.data.entity.AttendanceEntity

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getAttendanceByStudentAndDate(studentId: Int, date: Long): AttendanceEntity?

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND isPresent = 1")
    suspend fun getPresentCount(studentId: Int): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND isPresent = 0")
    suspend fun getAbsentCount(studentId: Int): Int

    // Get attendance for a specific class on a specific date
    @Query("""
        SELECT a.* FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date
    """)
    suspend fun getAttendanceByClassAndDate(classId: Int, date: Long): List<AttendanceEntity>

    // Count how many students have attendance marked for a class on a date
    @Query("""
        SELECT COUNT(*) FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date
    """)
    suspend fun getAttendanceCountForClass(classId: Int, date: Long): Int

    // Count present students for a class on a date
    @Query("""
        SELECT COUNT(*) FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date AND a.isPresent = 1
    """)
    suspend fun getPresentCountForClass(classId: Int, date: Long): Int

    // Count absent students for a class on a date
    @Query("""
        SELECT COUNT(*) FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date AND a.isPresent = 0
    """)
    suspend fun getAbsentCountForClass(classId: Int, date: Long): Int

    // Get total days in date range for a student
    @Query("""
        SELECT COUNT(DISTINCT date) FROM attendance 
        WHERE studentId = :studentId 
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getTotalDaysInRange(studentId: Int, startDate: Long, endDate: Long): Int

    // Get present count in date range for a student
    @Query("""
        SELECT COUNT(*) FROM attendance 
        WHERE studentId = :studentId 
        AND isPresent = 1 
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getPresentCountInRange(studentId: Int, startDate: Long, endDate: Long): Int

    // Get attendance records for a student in date range (for analytics)
    @Query("""
        SELECT * FROM attendance 
        WHERE studentId = :studentId 
        AND date >= :startDate 
        AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getAttendanceByStudentInRange(studentId: Int, startDate: Long, endDate: Long): List<AttendanceEntity>

    // Get all attendance records for a class in date range
    @Query("""
        SELECT a.* FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId 
        AND a.date >= :startDate 
        AND a.date <= :endDate
        ORDER BY a.date ASC
    """)
    suspend fun getAttendanceByClassInRange(classId: Int, startDate: Long, endDate: Long): List<AttendanceEntity>
}