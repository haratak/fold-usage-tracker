package io.github.haratak.foldusage.data

import io.github.haratak.foldusage.data.db.PostureIntervalDao
import io.github.haratak.foldusage.data.db.PostureIntervalEntity
import io.github.haratak.foldusage.domain.FoldPosture
import io.github.haratak.foldusage.domain.FoldReading
import io.github.haratak.foldusage.domain.PostureInterval
import io.github.haratak.foldusage.domain.PostureLog
import io.github.haratak.foldusage.domain.StoredInterval
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PostureRepository(
    private val dao: PostureIntervalDao,
) {
    private val mutex = Mutex()

    suspend fun record(reading: FoldReading, accept: () -> Boolean = { true }) = mutex.withLock {
        if (!accept()) return@withLock
        val opens = dao.getOpenIntervals()
        val extras = opens.dropLast(1)
        for (extra in extras) {
            dao.update(
                extra.copy(
                    endMillis = extra.lastSeenMillis.coerceAtLeast(extra.startMillis),
                ),
            )
        }
        val open = opens.lastOrNull()?.toStored()
        val result = PostureLog.apply(open, reading)
        val closed = result.closed
        if (closed != null && open != null) {
            dao.update(closed.toEntity(open.id))
            dao.insert(result.open.toEntity(id = 0))
        } else if (open == null) {
            dao.insert(result.open.toEntity(id = 0))
        } else {
            dao.update(result.open.toEntity(open.id))
        }
    }

    suspend fun closeOpen(nowMillis: Long) = mutex.withLock {
        val opens = dao.getOpenIntervals()
        for (open in opens) {
            val end = nowMillis.coerceAtLeast(open.startMillis)
            dao.update(open.copy(endMillis = end, lastSeenMillis = maxOf(open.lastSeenMillis, end)))
        }
    }

    suspend fun intervalsOverlapping(rangeStart: Long, rangeEnd: Long, nowMillis: Long): List<PostureInterval> {
        return dao.overlapping(rangeStart, rangeEnd).mapNotNull { entity ->
            val end = PostureLog.effectiveEnd(
                startMillis = entity.startMillis,
                endMillis = entity.endMillis,
                lastSeenMillis = entity.lastSeenMillis,
                nowMillis = nowMillis,
            )
            if (end <= entity.startMillis) return@mapNotNull null
            PostureInterval(
                posture = FoldPosture.fromStored(entity.posture),
                screenInteractive = entity.screenInteractive,
                startMillis = entity.startMillis,
                endMillis = end,
            )
        }
    }

    private fun PostureIntervalEntity.toStored(): StoredInterval {
        return StoredInterval(
            id = id,
            posture = FoldPosture.fromStored(posture),
            screenInteractive = screenInteractive,
            startMillis = startMillis,
            endMillis = endMillis,
            lastSeenMillis = lastSeenMillis,
            widthPx = widthPx,
            heightPx = heightPx,
        )
    }

    private fun StoredInterval.toEntity(id: Long): PostureIntervalEntity {
        return PostureIntervalEntity(
            id = id,
            posture = posture.name,
            screenInteractive = screenInteractive,
            startMillis = startMillis,
            endMillis = endMillis,
            lastSeenMillis = lastSeenMillis,
            widthPx = widthPx,
            heightPx = heightPx,
        )
    }
}
