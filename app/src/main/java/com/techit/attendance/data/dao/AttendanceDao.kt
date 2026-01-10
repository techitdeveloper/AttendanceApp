package com.techit.attendance.data.dao

import androidx.room.*
import com.techit.attendance.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getAttendance(studentId: Int, date: Long): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun getAttendanceByStudent(studentId: Int): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND isPresent = 1")
    suspend fun getPresentCount(studentId: Int): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND isPresent = 0")
    suspend fun getAbsentCount(studentId: Int): Int

    @Query("""
        SELECT a.* FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date
    """)
    suspend fun getAttendanceByClassAndDate(classId: Int, date: Long): List<AttendanceEntity>

    @Query("""
        SELECT * FROM attendance 
        WHERE studentId = :studentId 
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    suspend fun getAttendanceByDateRange(studentId: Int, startDate: Long, endDate: Long): List<AttendanceEntity>

    @Query("""
        SELECT COUNT(*) FROM attendance 
        WHERE studentId = :studentId 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalDaysInRange(studentId: Int, startDate: Long, endDate: Long): Int

    @Query("""
        SELECT COUNT(*) FROM attendance 
        WHERE studentId = :studentId 
        AND isPresent = 1
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getPresentCountInRange(studentId: Int, startDate: Long, endDate: Long): Int

    // NEW: Get count of students marked present for a class on a specific date
    @Query("""
        SELECT COUNT(*) FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date AND a.isPresent = 1
    """)
    suspend fun getPresentCountForClass(classId: Int, date: Long): Int

    // NEW: Get count of students marked absent for a class on a specific date
    @Query("""
        SELECT COUNT(*) FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date AND a.isPresent = 0
    """)
    suspend fun getAbsentCountForClass(classId: Int, date: Long): Int

    // NEW: Check if any attendance exists for a class on a date
    @Query("""
        SELECT COUNT(*) FROM attendance a
        INNER JOIN students s ON a.studentId = s.id
        WHERE s.classId = :classId AND a.date = :date
    """)
    suspend fun getAttendanceCountForClass(classId: Int, date: Long): Int
}