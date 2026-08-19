package dev.gaphunter.featureflagreferencecompanion.scan

import junit.framework.TestCase
import org.junit.Test

class FlagMethodNamesTest : TestCase() {

    @Test
    fun `test recognized SDK method names match`() {
        assertTrue(FlagMethodNames.matches("isEnabled"))
        assertTrue(FlagMethodNames.matches("isFeatureEnabled"))
        assertTrue(FlagMethodNames.matches("isFlagEnabled"))
        assertTrue(FlagMethodNames.matches("getFlag"))
        assertTrue(FlagMethodNames.matches("getBooleanFlag"))
        assertTrue(FlagMethodNames.matches("variation"))
        assertTrue(FlagMethodNames.matches("boolVariation"))
        assertTrue(FlagMethodNames.matches("isOn"))
        assertTrue(FlagMethodNames.matches("evaluateFlag"))
    }

    @Test
    fun `test matching is case insensitive`() {
        assertTrue(FlagMethodNames.matches("ISENABLED"))
        assertTrue(FlagMethodNames.matches("IsEnabled"))
        assertTrue(FlagMethodNames.matches("isenabled"))
    }

    @Test
    fun `test deliberately excluded generic names do not match`() {
        assertFalse(FlagMethodNames.matches("getValue"))
        assertFalse(FlagMethodNames.matches("get"))
        assertFalse(FlagMethodNames.matches("check"))
        assertFalse(FlagMethodNames.matches("test"))
        assertFalse(FlagMethodNames.matches("is"))
    }

    @Test
    fun `test unrelated method names do not match`() {
        assertFalse(FlagMethodNames.matches("setStatus"))
        assertFalse(FlagMethodNames.matches("println"))
        assertFalse(FlagMethodNames.matches(""))
    }
}
