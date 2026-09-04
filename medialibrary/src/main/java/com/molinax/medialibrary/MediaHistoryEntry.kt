package com.molinax.medialibrary

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_history")
data class MediaHistoryEntry(
    @PrimaryKey val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val lastPlayedAt: Long,
    val lastPositionSec: Int = 0
)
