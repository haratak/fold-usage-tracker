package io.github.haratak.foldusage.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity(
    tableName = "posture_intervals",
    indices = [Index("startMillis")],
)
data class PostureIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val posture: String,
    val screenInteractive: Boolean,
    val startMillis: Long,
    val endMillis: Long?,
    val lastSeenMillis: Long,
    val widthPx: Int,
    val heightPx: Int,
)

@Dao
interface PostureIntervalDao {
    @Query("SELECT * FROM posture_intervals WHERE endMillis IS NULL ORDER BY startMillis ASC")
    suspend fun getOpenIntervals(): List<PostureIntervalEntity>

    @Query(
        """
        SELECT * FROM posture_intervals
        WHERE startMillis < :rangeEnd
        AND (endMillis IS NULL OR endMillis > :rangeStart)
        ORDER BY startMillis ASC
        """,
    )
    suspend fun overlapping(rangeStart: Long, rangeEnd: Long): List<PostureIntervalEntity>

    @Insert
    suspend fun insert(entity: PostureIntervalEntity): Long

    @Update
    suspend fun update(entity: PostureIntervalEntity)
}
