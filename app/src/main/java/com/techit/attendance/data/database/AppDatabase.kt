package com.techit.attendance.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.techit.attendance.data.dao.AttendanceDao
import com.techit.attendance.data.dao.ClassDao
import com.techit.attendance.data.dao.StudentDao
import com.techit.attendance.data.entity.AttendanceEntity
import com.techit.attendance.data.entity.ClassEntity
import com.techit.attendance.data.entity.StudentEntity

@Database(
    entities = [ClassEntity::class, StudentEntity::class, AttendanceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}