package com.techit.attendance.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId")]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val classId: Int,
    val name: String,
    val rollIdentifier: String? = null
)