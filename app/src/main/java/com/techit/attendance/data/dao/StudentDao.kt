package com.techit.attendance.data.dao

import androidx.room.*
import com.techit.attendance.data.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    fun getStudentsByClass(classId: Int): Flow<List<StudentEntity>>

    @Insert
    suspend fun insertStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // NEW: Get count of students in a class
    @Query("SELECT COUNT(*) FROM students WHERE classId = :classId")
    suspend fun getStudentCount(classId: Int): Int
}