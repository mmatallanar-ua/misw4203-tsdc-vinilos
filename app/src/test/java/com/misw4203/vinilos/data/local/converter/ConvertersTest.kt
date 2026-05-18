package com.misw4203.vinilos.data.local.converter

import com.misw4203.vinilos.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `tracks round trip preserves data`() {
        val tracks = listOf(
            Track(1L, "Decisiones", "5:30"),
            Track(2L, "Desapariciones", "6:10"),
        )
        val json = converters.tracksToJson(tracks)
        assertEquals(tracks, converters.jsonToTracks(json))
    }

    @Test
    fun `jsonToTracks returns empty list for empty list json`() {
        assertEquals(emptyList<Track>(), converters.jsonToTracks(converters.tracksToJson(emptyList())))
    }

    @Test
    fun `jsonToTracks returns empty list for literal null`() {
        assertEquals(emptyList<Track>(), converters.jsonToTracks("null"))
    }

    @Test
    fun `jsonToTracks returns empty list for blank string`() {
        assertEquals(emptyList<Track>(), converters.jsonToTracks(""))
    }
}
