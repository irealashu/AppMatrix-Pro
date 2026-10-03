package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppCategory {
    SYSTEM, USER
}

@Entity(tableName = "app_notes")
data class AppNoteEntity(
    @PrimaryKey val packageName: String,
    val note: String,
    val customTags: String = ""
)

data class AppModel(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installTimeMs: Long,
    val updateTimeMs: Long,
    val apkSizeBytes: Long,
    val dataSizeEstimateBytes: Long,
    val category: AppCategory,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val customNote: String = ""
)
