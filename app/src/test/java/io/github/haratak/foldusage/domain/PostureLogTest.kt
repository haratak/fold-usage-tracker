package io.github.haratak.foldusage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostureLogTest {
    @Test
    fun sameStateWithinGraceOnlyUpdatesLastSeen() {
        val open = stored(posture = FoldPosture.FOLDED, screen = true, start = 0, lastSeen = 1_000)
        val result = PostureLog.apply(
            open,
            reading(FoldPosture.FOLDED, screen = true, at = 30_000, width = 1080, height = 2364),
        )
        assertNull(result.closed)
        assertEquals(30_000, result.open.lastSeenMillis)
        assertEquals(0, result.open.startMillis)
        assertEquals(1080, result.open.widthPx)
    }

    @Test
    fun screenOffClosesTheInteractiveInterval() {
        val open = stored(posture = FoldPosture.UNFOLDED, screen = true, start = 0, lastSeen = 4_000)
        val result = PostureLog.apply(
            open,
            reading(FoldPosture.UNFOLDED, screen = false, at = 5_000),
        )
        assertEquals(5_000L, result.closed?.endMillis)
        assertEquals(false, result.open.screenInteractive)
        assertEquals(5_000L, result.open.startMillis)
    }

    @Test
    fun staleOpenIntervalIsClosedAtLastSeen() {
        val open = stored(posture = FoldPosture.FOLDED, screen = true, start = 0, lastSeen = 10_000)
        val result = PostureLog.apply(
            open,
            reading(FoldPosture.FOLDED, screen = true, at = 10_000 + PostureLog.OPEN_GRACE_MILLIS + 1),
        )
        assertEquals(10_000L, result.closed?.endMillis)
        assertEquals(10_000 + PostureLog.OPEN_GRACE_MILLIS + 1, result.open.startMillis)
    }

    @Test
    fun freshOpenIntervalExtendsToNow() {
        val end = PostureLog.effectiveEnd(
            startMillis = 0,
            endMillis = null,
            lastSeenMillis = 50_000,
            nowMillis = 60_000,
        )
        assertEquals(60_000, end)
    }

    @Test
    fun deadOpenIntervalStopsAtLastSeen() {
        val end = PostureLog.effectiveEnd(
            startMillis = 0,
            endMillis = null,
            lastSeenMillis = 10_000,
            nowMillis = 10_000 + PostureLog.OPEN_GRACE_MILLIS + 5,
        )
        assertEquals(10_000, end)
    }

    private fun stored(
        posture: FoldPosture,
        screen: Boolean,
        start: Long,
        lastSeen: Long,
    ) = StoredInterval(
        id = 7,
        posture = posture,
        screenInteractive = screen,
        startMillis = start,
        endMillis = null,
        lastSeenMillis = lastSeen,
        widthPx = 100,
        heightPx = 200,
    )

    private fun reading(
        posture: FoldPosture,
        screen: Boolean,
        at: Long,
        width: Int = 0,
        height: Int = 0,
    ) = FoldReading(posture, screen, width, height, at)
}
