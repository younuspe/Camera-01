package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileType: String, // "PHOTO" or "VIDEO"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val locationStamp: String? = null,
    val isMotionTriggered: Boolean = false
)
