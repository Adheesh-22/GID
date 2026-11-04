package com.adheesh.gid.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// ── CaptureCategory ───────────────────────────────────────────────────────────
// What a capture means to the student: Resource, Idea, or one they added
// (SPEC §2.8). Default rows use the CaptureType name as id, which is what older
// captures already hold in `type`.
@Entity(tableName = "capture_categories")
data class CaptureCategoryEntity(
    @PrimaryKey val id : String,
    val name           : String,
    val sortOrder      : Int,
    val createdAt      : Long,
    val updatedAt      : Long,
) {
    /** Deadline carries the due date, so it can be renamed but never removed. */
    val isRemovable: Boolean get() = id != CaptureType.DEADLINE.name
}

@Dao
interface CaptureCategoryDao {

    @Query("SELECT * FROM capture_categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CaptureCategoryEntity>>

    @Query("SELECT * FROM capture_categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CaptureCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CaptureCategoryEntity)

    @Query("DELETE FROM capture_categories WHERE id = :id")
    suspend fun deleteById(id: String)
}
