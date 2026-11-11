package bo.edu.lsb.quechua.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SignDebouncerTest {

    @Test
    fun testDebouncerRequiresExact12Samples() {
        var acceptedSign: String? = null
        val debouncer = SignDebouncer(threshold = 12) { sign ->
            acceptedSign = sign
        }

        // Send 11 samples: should not trigger
        for (i in 1..11) {
            debouncer.processReading("AGUA")
            assertEquals(null, acceptedSign)
            assertEquals("AGUA", debouncer.candidateGloss)
            assertEquals(i, debouncer.stableCount)
        }

        // 12th sample: must trigger
        debouncer.processReading("AGUA")
        assertEquals("AGUA", acceptedSign)
    }

    @Test
    fun testDebouncerResetsOnNoiseOrInterruption() {
        var acceptedSign: String? = null
        val debouncer = SignDebouncer(threshold = 12) { sign ->
            acceptedSign = sign
        }

        // Send 8 samples of AGUA
        for (i in 1..8) {
            debouncer.processReading("AGUA")
        }
        assertEquals(8, debouncer.stableCount)

        // Noise / Glitch sample: NINGUNA
        debouncer.processReading("NINGUNA")
        assertEquals(0, debouncer.stableCount)
        assertEquals("NINGUNA", debouncer.candidateGloss)
        assertEquals(null, acceptedSign)

        // Send 12 samples of PAN
        for (i in 1..12) {
            debouncer.processReading("PAN")
        }
        assertEquals("PAN", acceptedSign)
    }
}
