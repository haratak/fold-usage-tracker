package io.github.haratak.foldusage.domain

/**
 * Classifies the lit internal display by aspect ratio.
 *
 * Pixel 10 Pro Fold cover is 1080×2364 (about 2.19) and the inner panel is
 * 2076×2152 (about 1.04). A threshold of 1.6 sits between those, and also
 * between the Pixel Fold and Pixel 9 Pro Fold panels. Physical pixels are
 * used, so rotation does not change the result.
 *
 * Tabletop / half-open keeps the inner panel lit, so it is counted as unfolded.
 */
object DisplayPostureClassifier {
    const val FOLDED_MIN_ASPECT = 1.6

    fun classify(widthPx: Int, heightPx: Int): FoldPosture {
        if (widthPx <= 0 || heightPx <= 0) return FoldPosture.UNKNOWN
        val longSide = maxOf(widthPx, heightPx).toDouble()
        val shortSide = minOf(widthPx, heightPx).toDouble()
        if (shortSide <= 0.0) return FoldPosture.UNKNOWN
        val aspect = longSide / shortSide
        return if (aspect >= FOLDED_MIN_ASPECT) FoldPosture.FOLDED else FoldPosture.UNFOLDED
    }

    fun selectActive(displays: List<DisplaySample>): DisplaySample? {
        return displays
            .asSequence()
            .filter { it.isInternal && it.power == DisplayPower.ON }
            .filter { it.widthPx > 0 && it.heightPx > 0 }
            .maxWithOrNull(
                compareBy<DisplaySample> { it.widthPx.toLong() * it.heightPx.toLong() }
                    .thenByDescending { it.id },
            )
    }
}

object FoldReadingResolver {
    fun resolve(
        displays: List<DisplaySample>,
        screenInteractive: Boolean,
        lastPosture: FoldPosture,
        nowMillis: Long,
    ): FoldReading {
        val active = DisplayPostureClassifier.selectActive(displays)
        return if (active != null) {
            FoldReading(
                posture = DisplayPostureClassifier.classify(active.widthPx, active.heightPx),
                screenInteractive = screenInteractive,
                widthPx = active.widthPx,
                heightPx = active.heightPx,
                atMillis = nowMillis,
            )
        } else {
            FoldReading(
                posture = lastPosture,
                screenInteractive = screenInteractive,
                widthPx = 0,
                heightPx = 0,
                atMillis = nowMillis,
            )
        }
    }
}
