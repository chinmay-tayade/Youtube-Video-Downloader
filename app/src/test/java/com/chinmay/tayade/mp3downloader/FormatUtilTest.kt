package com.chinmay.tayade.mp3downloader

import com.chinmay.tayade.mp3downloader.util.formatBytes
import com.chinmay.tayade.mp3downloader.util.formatCountAbbreviated
import com.chinmay.tayade.mp3downloader.util.formatDuration
import com.chinmay.tayade.mp3downloader.util.formatSpeed
import com.chinmay.tayade.mp3downloader.util.hostOf
import com.chinmay.tayade.mp3downloader.util.isYouTubeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilTest {

    @Test
    fun countAbbreviation() {
        assertEquals("0", formatCountAbbreviated(0))
        assertEquals("999", formatCountAbbreviated(999))
        assertEquals("1.23K", formatCountAbbreviated(1234))
        assertEquals("1.50M", formatCountAbbreviated(1_500_000))
    }

    @Test
    fun byteFormatting() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("1.5 MB", formatBytes(1024L * 1024 * 3 / 2))
    }

    @Test
    fun speedFormatting() {
        assertEquals("—", formatSpeed(0))
        assertEquals("1.0 MB/s", formatSpeed(1024L * 1024))
    }

    @Test
    fun durationFormatting() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("1:01", formatDuration(61))
        assertEquals("1:01:01", formatDuration(3661))
        assertEquals("0:00", formatDuration(-5))
    }

    @Test
    fun hostExtraction() {
        assertEquals("youtu.be", hostOf("https://youtu.be/abc123"))
        assertEquals("www.youtube.com", hostOf("https://www.youtube.com/watch?v=abc"))
        assertEquals("m.youtube.com", hostOf("m.youtube.com/watch?v=abc"))
        assertEquals("example.com", hostOf("http://user:pw@example.com:8080/path"))
        assertNull(hostOf(""))
        assertNull(hostOf(null))
    }

    @Test
    fun youTubeDetection() {
        assertTrue(isYouTubeUrl("https://youtu.be/abc"))
        assertTrue(isYouTubeUrl("https://www.youtube.com/shorts/xyz"))
        assertTrue(isYouTubeUrl("https://music.youtube.com/watch?v=x"))
        assertFalse(isYouTubeUrl("https://vimeo.com/12345"))
        assertFalse(isYouTubeUrl("not a url"))
    }
}
