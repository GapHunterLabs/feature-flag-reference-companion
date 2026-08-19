package dev.gaphunter.featureflagreferencecompanion.model

import junit.framework.TestCase
import org.junit.Test

class FlagReferenceVerdictTest : TestCase() {

    @Test
    fun `test a single reference is an orphan candidate`() {
        val result = FlagReferenceResult(key = "checkout_v2", totalReferenceCount = 1)
        assertEquals(FlagReferenceVerdict.ORPHAN_CANDIDATE, result.verdict)
    }

    @Test
    fun `test two or more references are in use`() {
        val two = FlagReferenceResult(key = "checkout_v2", totalReferenceCount = 2)
        val many = FlagReferenceResult(key = "checkout_v2", totalReferenceCount = 15)
        assertEquals(FlagReferenceVerdict.IN_USE, two.verdict)
        assertEquals(FlagReferenceVerdict.IN_USE, many.verdict)
    }
}
