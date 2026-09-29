package io.github.haratak.foldusage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayPostureClassifierTest {
    @Test
    fun pixel10ProFoldCoverIsFolded() {
        assertEquals(FoldPosture.FOLDED, DisplayPostureClassifier.classify(1080, 2364))
        assertEquals(FoldPosture.FOLDED, DisplayPostureClassifier.classify(2364, 1080))
    }

    @Test
    fun pixel10ProFoldInnerIsUnfolded() {
        assertEquals(FoldPosture.UNFOLDED, DisplayPostureClassifier.classify(2076, 2152))
        assertEquals(FoldPosture.UNFOLDED, DisplayPostureClassifier.classify(2152, 2076))
    }

    @Test
    fun aspectThreshold() {
        assertEquals(FoldPosture.FOLDED, DisplayPostureClassifier.classify(1600, 1000))
        assertEquals(FoldPosture.UNFOLDED, DisplayPostureClassifier.classify(1599, 1000))
    }

    @Test
    fun zeroSizeIsUnknown() {
        assertEquals(FoldPosture.UNKNOWN, DisplayPostureClassifier.classify(0, 2364))
        assertEquals(FoldPosture.UNKNOWN, DisplayPostureClassifier.classify(1080, 0))
    }

    @Test
    fun selectActiveIgnoresExternalAndOffDisplays() {
        val cover = sample(id = 1, internal = true, power = DisplayPower.ON, 1080, 2364)
        val innerOff = sample(id = 0, internal = true, power = DisplayPower.OFF, 2076, 2152)
        val external = sample(id = 2, internal = false, power = DisplayPower.ON, 1920, 1080)
        assertEquals(cover, DisplayPostureClassifier.selectActive(listOf(innerOff, external, cover)))
    }

    @Test
    fun selectActivePrefersLargestLitInternalDisplay() {
        val cover = sample(id = 1, internal = true, power = DisplayPower.ON, 1080, 2364)
        val inner = sample(id = 0, internal = true, power = DisplayPower.ON, 2076, 2152)
        assertEquals(inner, DisplayPostureClassifier.selectActive(listOf(cover, inner)))
    }

    @Test
    fun selectActiveReturnsNullWhenNothingIsLit() {
        val off = sample(id = 0, internal = true, power = DisplayPower.OFF, 2076, 2152)
        assertNull(DisplayPostureClassifier.selectActive(listOf(off)))
    }

    @Test
    fun screenOffKeepsLastPosture() {
        val reading = FoldReadingResolver.resolve(
            displays = listOf(sample(0, true, DisplayPower.OFF, 2076, 2152)),
            screenInteractive = false,
            lastPosture = FoldPosture.UNFOLDED,
            nowMillis = 50,
        )
        assertEquals(FoldPosture.UNFOLDED, reading.posture)
        assertEquals(false, reading.screenInteractive)
        assertEquals(0, reading.widthPx)
    }

    @Test
    fun litCoverBecomesFolded() {
        val reading = FoldReadingResolver.resolve(
            displays = listOf(
                sample(0, true, DisplayPower.OFF, 2076, 2152),
                sample(1, true, DisplayPower.ON, 1080, 2364),
            ),
            screenInteractive = true,
            lastPosture = FoldPosture.UNFOLDED,
            nowMillis = 80,
        )
        assertEquals(FoldPosture.FOLDED, reading.posture)
        assertEquals(1080, reading.widthPx)
        assertEquals(2364, reading.heightPx)
    }

    private fun sample(
        id: Int,
        internal: Boolean,
        power: DisplayPower,
        width: Int,
        height: Int,
    ) = DisplaySample(id, internal, power, width, height)
}
