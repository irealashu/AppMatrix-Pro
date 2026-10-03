package com.example.data

import androidx.room.*
import com.example.model.AppNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNoteDao {
    @Query("SELECT * FROM app_notes")
    fun getAllNotes(): Flow<List<AppNoteEntity>>

    @Query("SELECT * FROM app_notes WHERE packageName = :pkg LIMIT 1")
    suspend fun getNoteForPackage(pkg: String): AppNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: AppNoteEntity)
}

@Database(entities = [AppNoteEntity::class], version = 3, exportSchema = false)
abstract class AppMatrixDatabase : RoomDatabase() {
    abstract fun appNoteDao(): AppNoteDao
}
