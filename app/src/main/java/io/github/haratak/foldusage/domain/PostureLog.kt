package io.github.haratak.foldusage.domain

data class PostureLogResult(
    val closed: StoredInterval?,
    val open: StoredInterval,
)

object PostureLog {
    const val OPEN_GRACE_MILLIS = 90_000L

    fun apply(
        open: StoredInterval?,
        reading: FoldReading,
        graceMillis: Long = OPEN_GRACE_MILLIS,
    ): PostureLogResult {
        val fresh = StoredInterval(
            id = 0,
            posture = reading.posture,
            screenInteractive = reading.screenInteractive,
            startMillis = reading.atMillis,
            endMillis = null,
            lastSeenMillis = reading.atMillis,
            widthPx = reading.widthPx,
            heightPx = reading.heightPx,
        )
        if (open == null) {
            return PostureLogResult(closed = null, open = fresh)
        }
        if (reading.atMillis < open.startMillis) {
            return PostureLogResult(closed = closeAtLastSeen(open), open = fresh)
        }
        val sameState = open.posture == reading.posture &&
            open.screenInteractive == reading.screenInteractive
        if (sameState && reading.atMillis - open.lastSeenMillis <= graceMillis) {
            return PostureLogResult(
                closed = null,
                open = open.copy(
                    lastSeenMillis = reading.atMillis,
                    widthPx = reading.widthPx.takeIf { it > 0 } ?: open.widthPx,
                    heightPx = reading.heightPx.takeIf { it > 0 } ?: open.heightPx,
                ),
            )
        }
        if (sameState) {
            return PostureLogResult(closed = closeAtLastSeen(open), open = fresh)
        }
        val end = reading.atMillis.coerceAtLeast(open.startMillis)
        return PostureLogResult(
            closed = open.copy(endMillis = end, lastSeenMillis = maxOf(open.lastSeenMillis, end)),
            open = fresh,
        )
    }

    fun effectiveEnd(
        startMillis: Long,
        endMillis: Long?,
        lastSeenMillis: Long,
        nowMillis: Long,
        graceMillis: Long = OPEN_GRACE_MILLIS,
    ): Long {
        if (endMillis != null) return endMillis
        val seen = lastSeenMillis.coerceAtLeast(startMillis)
        return if (nowMillis - seen <= graceMillis) nowMillis else seen
    }

    private fun closeAtLastSeen(open: StoredInterval): StoredInterval {
        val end = open.lastSeenMillis.coerceAtLeast(open.startMillis)
        return open.copy(endMillis = end, lastSeenMillis = end)
    }
}
