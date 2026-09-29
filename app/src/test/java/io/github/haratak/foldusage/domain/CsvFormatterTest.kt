package io.github.haratak.foldusage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class CsvFormatterTest {
    @Test
    fun writesHeaderAndEscapesLabels() {
        val csv = CsvFormatter.format(
            slices = listOf(
                UsageSlice("com.a", FoldPosture.FOLDED, 0, 60_000),
            ),
            labels = mapOf("com.a" to "地図, ナビ"),
            zone = ZoneId.of("Asia/Tokyo"),
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals(
            "\uFEFFstart,end,package,app_label,fold_state\n" +
                "1970-01-01T09:00:00+09:00,1970-01-01T09:01:00+09:00,com.a,\"地図, ナビ\",folded\n",
            csv,
        )
    }

    @Test
    fun durationFormat() {
        assertEquals("0分", formatDurationJa(0))
        assertEquals("1分未満", formatDurationJa(59_999))
        assertEquals("1分", formatDurationJa(60_000))
        assertEquals("1時間", formatDurationJa(3_600_000))
        assertEquals("1時間30分", formatDurationJa(5_400_000))
    }
}
