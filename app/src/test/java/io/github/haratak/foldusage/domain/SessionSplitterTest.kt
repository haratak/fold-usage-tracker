package io.github.haratak.foldusage.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSplitterTest {
    @Test
    fun splitsASessionAcrossAFold() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(ForegroundSession("com.chrome", 0, 10_000)),
            intervals = listOf(
                interval(FoldPosture.FOLDED, screen = true, 0, 3_000),
                interval(FoldPosture.UNFOLDED, screen = true, 3_000, 10_000),
            ),
        )
        assertEquals(3_000, summary.foldedTotalMillis)
        assertEquals(7_000, summary.unfoldedTotalMillis)
        assertEquals(
            listOf(
                UsageSlice("com.chrome", FoldPosture.FOLDED, 0, 3_000),
                UsageSlice("com.chrome", FoldPosture.UNFOLDED, 3_000, 10_000),
            ),
            summary.slices,
        )
        assertEquals(1, summary.transitionCount)
    }

    @Test
    fun screenOffTimeIsNotCounted() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(ForegroundSession("com.a", 0, 10_000)),
            intervals = listOf(
                interval(FoldPosture.FOLDED, screen = true, 0, 4_000),
                interval(FoldPosture.FOLDED, screen = false, 4_000, 7_000),
                interval(FoldPosture.UNFOLDED, screen = true, 7_000, 10_000),
            ),
        )
        assertEquals(4_000, summary.foldedTotalMillis)
        assertEquals(3_000, summary.unfoldedTotalMillis)
        assertEquals(
            listOf(
                UsageSlice("com.a", FoldPosture.FOLDED, 0, 4_000),
                UsageSlice("com.a", FoldPosture.UNFOLDED, 7_000, 10_000),
            ),
            summary.slices,
        )
        assertEquals(1, summary.transitionCount)
    }

    @Test
    fun sessionEntirelyDuringScreenOffContributesNothing() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(ForegroundSession("com.a", 0, 5_000)),
            intervals = listOf(interval(FoldPosture.FOLDED, screen = false, 0, 5_000)),
        )
        assertEquals(0, summary.foldedTotalMillis)
        assertEquals(0, summary.unfoldedTotalMillis)
        assertEquals(emptyList<UsageSlice>(), summary.slices)
    }

    @Test
    fun partialOverlapUsesOnlyTheIntersection() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(ForegroundSession("com.a", 1_000, 5_000)),
            intervals = listOf(
                interval(FoldPosture.FOLDED, screen = true, 0, 2_000),
                interval(FoldPosture.UNFOLDED, screen = true, 2_000, 8_000),
            ),
        )
        assertEquals(
            listOf(
                UsageSlice("com.a", FoldPosture.FOLDED, 1_000, 2_000),
                UsageSlice("com.a", FoldPosture.UNFOLDED, 2_000, 5_000),
            ),
            summary.slices,
        )
    }

    @Test
    fun aggregatesSeveralAppsPerState() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(
                ForegroundSession("com.b", 0, 2_000),
                ForegroundSession("com.a", 2_000, 8_000),
                ForegroundSession("com.b", 8_000, 9_000),
            ),
            intervals = listOf(
                interval(FoldPosture.FOLDED, screen = true, 0, 4_000),
                interval(FoldPosture.UNFOLDED, screen = true, 4_000, 9_000),
            ),
        )
        assertEquals(
            listOf(AppDuration("com.a", 2_000), AppDuration("com.b", 2_000)),
            summary.folded,
        )
        assertEquals(
            listOf(AppDuration("com.a", 4_000), AppDuration("com.b", 1_000)),
            summary.unfolded,
        )
    }

    @Test
    fun unknownPostureIsNotCounted() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(ForegroundSession("com.a", 0, 5_000)),
            intervals = listOf(interval(FoldPosture.UNKNOWN, screen = true, 0, 5_000)),
        )
        assertEquals(UsageSummary.EMPTY.copy(transitionCount = 0), summary.copy(transitionCount = 0))
        assertEquals(0, summary.foldedTotalMillis)
        assertEquals(emptyList<UsageSlice>(), summary.slices)
    }

    @Test
    fun screenTogglesAreNotFoldTransitions() {
        val intervals = listOf(
            interval(FoldPosture.FOLDED, screen = true, 0, 1_000),
            interval(FoldPosture.FOLDED, screen = false, 1_000, 2_000),
            interval(FoldPosture.FOLDED, screen = true, 2_000, 3_000),
            interval(FoldPosture.UNFOLDED, screen = true, 3_000, 4_000),
            interval(FoldPosture.UNKNOWN, screen = true, 4_000, 4_500),
            interval(FoldPosture.UNFOLDED, screen = true, 4_500, 5_000),
        )
        assertEquals(1, SessionSplitter.transitionCount(intervals))
    }

    @Test
    fun overlappingIntervalsAreNotDoubleCounted() {
        val summary = SessionSplitter.summarize(
            sessions = listOf(ForegroundSession("com.a", 0, 8_000)),
            intervals = listOf(
                interval(FoldPosture.UNFOLDED, screen = true, 0, 5_000),
                interval(FoldPosture.UNFOLDED, screen = true, 3_000, 8_000),
            ),
        )
        assertEquals(8_000, summary.unfoldedTotalMillis)
    }

    private fun interval(
        posture: FoldPosture,
        screen: Boolean,
        start: Long,
        end: Long,
    ) = PostureInterval(posture, screen, start, end)
}
