package io.github.haratak.foldusage.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageSessionReconstructorTest {
    @Test
    fun resumeAndPauseBecomeOneSession() {
        val sessions = UsageSessionReconstructor.reconstruct(
            events = listOf(
                event(0, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(5_000, "com.a", UsageEventType.ACTIVITY_PAUSED),
            ),
            rangeEndMillis = 9_000,
        )
        assertEquals(listOf(ForegroundSession("com.a", 0, 5_000)), sessions)
    }

    @Test
    fun switchingAppsClosesThePreviousSession() {
        val sessions = UsageSessionReconstructor.reconstruct(
            events = listOf(
                event(0, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(1_000, "com.b", UsageEventType.ACTIVITY_RESUMED),
                event(1_100, "com.a", UsageEventType.ACTIVITY_PAUSED),
                event(3_000, "com.b", UsageEventType.ACTIVITY_PAUSED),
            ),
            rangeEndMillis = 9_000,
        )
        assertEquals(
            listOf(
                ForegroundSession("com.a", 0, 1_000),
                ForegroundSession("com.b", 1_000, 3_000),
            ),
            sessions,
        )
    }

    @Test
    fun screenOffEndsTheSessionAndBlocksMerging() {
        val sessions = UsageSessionReconstructor.reconstruct(
            events = listOf(
                event(0, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(5_000, "", UsageEventType.SCREEN_NON_INTERACTIVE),
                event(8_000, "", UsageEventType.SCREEN_INTERACTIVE),
                event(8_000, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(12_000, "com.a", UsageEventType.ACTIVITY_PAUSED),
            ),
            rangeEndMillis = 20_000,
        )
        assertEquals(
            listOf(
                ForegroundSession("com.a", 0, 5_000),
                ForegroundSession("com.a", 8_000, 12_000),
            ),
            sessions,
        )
    }

    @Test
    fun shortGapOfTheSameAppIsMerged() {
        val sessions = UsageSessionReconstructor.reconstruct(
            events = listOf(
                event(0, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(1_000, "com.a", UsageEventType.ACTIVITY_PAUSED),
                event(2_000, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(4_000, "com.a", UsageEventType.ACTIVITY_STOPPED),
            ),
            rangeEndMillis = 9_000,
        )
        assertEquals(listOf(ForegroundSession("com.a", 0, 4_000)), sessions)
    }

    @Test
    fun openSessionIsClosedAtRangeEnd() {
        val sessions = UsageSessionReconstructor.reconstruct(
            events = listOf(event(1_000, "com.a", UsageEventType.ACTIVITY_RESUMED)),
            rangeEndMillis = 5_000,
        )
        assertEquals(listOf(ForegroundSession("com.a", 1_000, 5_000)), sessions)
    }

    @Test
    fun pauseOfAnotherPackageIsIgnored() {
        val sessions = UsageSessionReconstructor.reconstruct(
            events = listOf(
                event(0, "com.a", UsageEventType.ACTIVITY_RESUMED),
                event(500, "com.b", UsageEventType.ACTIVITY_PAUSED),
                event(1_000, "com.a", UsageEventType.ACTIVITY_PAUSED),
            ),
            rangeEndMillis = 2_000,
        )
        assertEquals(listOf(ForegroundSession("com.a", 0, 1_000)), sessions)
    }

    @Test
    fun clipTrimsSessionsToTheRequestedRange() {
        val clipped = UsageSessionReconstructor.clip(
            sessions = listOf(ForegroundSession("com.a", 0, 10_000)),
            rangeStartMillis = 2_000,
            rangeEndMillis = 6_000,
        )
        assertEquals(listOf(ForegroundSession("com.a", 2_000, 6_000)), clipped)
    }

    private fun event(time: Long, packageName: String, type: UsageEventType) =
        RawUsageEvent(time, packageName, type)
}
