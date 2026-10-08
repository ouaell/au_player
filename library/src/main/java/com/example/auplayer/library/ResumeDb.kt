package com.example.auplayer.library

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Room schema (MS-019). DAO tests run on Robolectric/T2 (MS-060); JVM tests
 * cover [InMemoryResumeStore] which shares the [ResumeStore] contract.
 */
@Entity(tableName = "resume")
data class ResumeEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long
)

@Dao
interface ResumeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ResumeEntity)

    @Query("SELECT * FROM resume WHERE uri = :uri LIMIT 1")
    suspend fun load(uri: String): ResumeEntity?

    @Query("DELETE FROM resume WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Query("DELETE FROM resume")
    suspend fun clear()

    @Query(
        "SELECT * FROM resume WHERE durationMs > 0 AND positionMs > 0 " +
            "AND positionMs < durationMs ORDER BY updatedAtMs DESC LIMIT :limit"
    )
    suspend fun continueWatching(limit: Int): List<ResumeEntity>
}

@Database(entities = [ResumeEntity::class], version = 1, exportSchema = false)
abstract class ResumeDatabase : androidx.room.RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
}

/** [ResumeStore] over the Room DAO (runtime wiring lands with the database builder). */
class RoomResumeStore(private val dao: ResumeDao) : ResumeStore {
    override suspend fun save(entry: ResumeEntry) {
        dao.upsert(
            ResumeEntity(entry.uri, entry.title, entry.positionMs, entry.durationMs, entry.updatedAtMs)
        )
    }

    override suspend fun load(uri: String): ResumeEntry? = dao.load(uri)?.toEntry()

    override suspend fun remove(uri: String) = dao.remove(uri)

    override suspend fun clear() = dao.clear()

    override suspend fun continueWatching(limit: Int): List<ResumeEntry> =
        dao.continueWatching(limit).map { it.toEntry() }

    private fun ResumeEntity.toEntry() =
        ResumeEntry(uri, title, positionMs, durationMs, updatedAtMs)
}
