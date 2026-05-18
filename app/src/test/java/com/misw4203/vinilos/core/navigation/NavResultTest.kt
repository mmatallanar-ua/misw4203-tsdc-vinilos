package com.misw4203.vinilos.core.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the nav-result helper. Only the [SavedStateHandle]
 * extensions are exercised here; the [androidx.navigation.NavController] /
 * `@Composable` surface needs an instrumented host and is out of scope.
 */
class NavResultTest {

    private val key = "refresh_test_key"

    @Test
    fun `setRefresh leaves the flag true`() {
        val handle = SavedStateHandle()

        handle.setRefresh(key)

        assertEquals(true, handle.get<Boolean>(key))
    }

    @Test
    fun `clearRefresh leaves the flag false`() {
        val handle = SavedStateHandle()
        handle.setRefresh(key)

        handle.clearRefresh(key)

        assertEquals(false, handle.get<Boolean>(key))
    }

    @Test
    fun `getStateFlow is false initially then true after setRefresh`() {
        val handle = SavedStateHandle()
        val flow = handle.getStateFlow(key, false)

        assertFalse(flow.value)

        handle.setRefresh(key)

        assertTrue(flow.value)
    }

    @Test
    fun `set then clear round-trips the state flow back to false`() {
        val handle = SavedStateHandle()
        val flow = handle.getStateFlow(key, false)

        handle.setRefresh(key)
        assertTrue(flow.value)

        handle.clearRefresh(key)
        assertFalse(flow.value)
    }
}
