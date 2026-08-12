package com.example.data.repository

import com.example.data.local.MediaItem
import com.example.data.local.MediaItemDao
import com.example.data.local.MotionEvent
import com.example.data.local.MotionEventDao
import kotlinx.coroutines.flow.Flow

class SecurityRepository(
    private val mediaItemDao: MediaItemDao,
    private val motionEventDao: MotionEventDao
) {
    val allMedia: Flow<List<MediaItem>> = mediaItemDao.getAllMedia()
    val allMotionEvents: Flow<List<MotionEvent>> = motionEventDao.getAllEvents()

    fun getMediaByType(type: String): Flow<List<MediaItem>> = mediaItemDao.getMediaByType(type)

    suspend fun saveMedia(item: MediaItem): Long = mediaItemDao.insertMedia(item)

    suspend fun deleteMedia(item: MediaItem) = mediaItemDao.deleteMedia(item)

    suspend fun deleteMediaById(id: Long) = mediaItemDao.deleteById(id)

    suspend fun logMotionEvent(event: MotionEvent): Long = motionEventDao.insertEvent(event)

    suspend fun clearMotionEvents() = motionEventDao.clearAll()
}
