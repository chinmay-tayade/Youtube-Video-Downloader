package com.chinmay.tayade.mp3downloader

import com.chinmay.tayade.mp3downloader.data.model.MediaSelection
import com.chinmay.tayade.mp3downloader.util.toFormatSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSpecTest {

    @Test
    fun auto_prefers_progressive_then_falls_back() {
        assertEquals(
            "best[vcodec!=none][acodec!=none]/best/worst",
            MediaSelection.Auto.toFormatSelector(),
        )
    }

    @Test
    fun video_caps_height_and_prefers_mp4_progressive() {
        val selector = MediaSelection.Video(720).toFormatSelector()
        assertTrue(selector.startsWith("best[height<=720][ext=mp4][vcodec!=none][acodec!=none]"))
        assertTrue(selector.endsWith("/best/worst"))
        assertTrue(selector.contains("[height<=720][vcodec!=none][acodec!=none]"))
    }

    @Test
    fun video_never_emits_a_zero_or_negative_cap() {
        val selector = MediaSelection.Video(0).toFormatSelector()
        assertTrue(selector.contains("height<=1"))
    }

    @Test
    fun audio_m4a_targets_the_m4a_container_first_then_falls_back_to_progressive() {
        val selector = MediaSelection.Audio("m4a").toFormatSelector()
        assertTrue(selector.startsWith("bestaudio[ext=m4a]/bestaudio[acodec^=mp4a]/bestaudio/"))
        assertTrue(selector.endsWith("/best[vcodec!=none][acodec!=none]/best/worst"))
    }

    @Test
    fun audio_unknown_ext_still_produces_a_valid_selector() {
        val selector = MediaSelection.Audio("flac").toFormatSelector()
        assertTrue(selector.startsWith("bestaudio[ext=flac]/bestaudio/"))
    }

    @Test
    fun labels_are_human_readable() {
        assertEquals("Auto (best)", MediaSelection.Auto.label)
        assertEquals("720p", MediaSelection.Video(720).label)
        assertEquals("Audio · OPUS", MediaSelection.Audio("opus").label)
    }
}
