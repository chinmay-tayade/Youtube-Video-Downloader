package com.chinmay.tayade.mp3downloader.Utility

import android.util.Patterns
import android.widget.ImageView
import com.chinmay.tayade.mp3downloader.R
import com.squareup.picasso.Picasso
import kotlin.math.abs
import kotlin.math.ln

class UtilityFunction {

    companion object {
        private val UNITS = arrayOf("K", "M", "B", "T")
    }

    /**
     * Lightweight, offline check that a string looks like a web URL.
     * Does not open a network connection (that would crash on the main thread).
     */
    internal fun isUrl(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        return Patterns.WEB_URL.matcher(input.trim()).matches()
    }

    internal fun formatNumberAbbreviated(number: Long?): String {
        val value = number ?: 0L
        if (abs(value) < 1000) return value.toString()

        val suffixIndex = (ln(abs(value).toDouble()) / ln(1000.0)).toInt()
        val safeIndex = suffixIndex.coerceIn(1, UNITS.size)

        val abbreviated = value / Math.pow(1000.0, safeIndex.toDouble())
        return "%.2f%s".format(abbreviated, UNITS[safeIndex - 1])
    }

    fun loadYouTubeThumbnail(url: String?, imageView: ImageView) {
        if (url.isNullOrBlank()) {
            Picasso.get().cancelRequest(imageView)
            return
        }
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.youtubeplayer)
            .error(R.drawable.youtubeplayer)
            .fit()
            .centerCrop()
            .into(imageView)
    }
}
