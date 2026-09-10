package com.chinmay.tayade.mp3downloader.Screens

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chinmay.tayade.mp3downloader.Fragments.Fragment1
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.Utility.UtilityFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DownloadingScreen : AppCompatActivity() {

    private val utils = UtilityFunction()
    private val statusCard = Fragment1()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloading_screen)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }

        val link = intent?.getStringExtra("youtube_link").orEmpty()
        val locationUri = intent?.getStringExtra("location_uri").orEmpty()

        val thumbnail = findViewById<ImageView>(R.id.thumbnail)
        val nameOfVideo = findViewById<TextView>(R.id.name_of_video)
        val viewCounts = findViewById<TextView>(R.id.view_counts)
        val likesCounts = findViewById<TextView>(R.id.like_counts)

        if (link.isBlank()) {
            Toast.makeText(this, "No video link was received", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        showFragment(statusCard)
        statusCard.setStatus("Grabbing info…")

        lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { fetchVideoInfo(link) }

            if (!info.ok) {
                statusCard.setStatus("Couldn't load video", finished = true)
                Toast.makeText(
                    this@DownloadingScreen,
                    info.error.ifBlank { "Could not load video information" },
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@launch
            }

            utils.loadYouTubeThumbnail(info.thumbnail, thumbnail)
            nameOfVideo.text = info.title
            viewCounts.text = "${utils.formatNumberAbbreviated(info.views)} Views"
            likesCounts.text = "${utils.formatNumberAbbreviated(info.likes)} Likes"

            statusCard.setStatus("Downloading…")
            val result = withContext(Dispatchers.IO) { downloadVideo(link, locationUri) }

            if (isFinishing || isDestroyed) return@launch
            val done = if (result.ok) "Download complete" else "Download failed"
            statusCard.setStatus(result.message.ifBlank { done }, finished = true)
            Toast.makeText(this@DownloadingScreen, done, Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchVideoInfo(link: String): VideoInfo {
        return try {
            val json = pythonModule().callAttr("get_video_info", link).toString()
            val obj = JSONObject(json)
            VideoInfo(
                ok = obj.optBoolean("ok", false),
                title = obj.optString("title", "Unknown title"),
                thumbnail = obj.optString("thumbnail", ""),
                views = obj.optLong("views", 0L),
                likes = obj.optLong("likes", 0L),
                error = obj.optString("error", "")
            )
        } catch (e: Exception) {
            VideoInfo(false, "", "", 0L, 0L, e.message ?: "Unexpected error")
        }
    }

    private fun downloadVideo(link: String, destination: String): DownloadResult {
        return try {
            val json = pythonModule().callAttr("download_video", link, destination).toString()
            val obj = JSONObject(json)
            DownloadResult(obj.optBoolean("ok", false), obj.optString("message", ""))
        } catch (e: Exception) {
            DownloadResult(false, e.message ?: "Unexpected error")
        }
    }

    private fun pythonModule() = Python.getInstance().getModule("video_downloader")

    private fun showFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.changing_frame, fragment)
            .commitAllowingStateLoss()
    }

    private data class VideoInfo(
        val ok: Boolean,
        val title: String,
        val thumbnail: String,
        val views: Long,
        val likes: Long,
        val error: String
    )

    private data class DownloadResult(val ok: Boolean, val message: String)
}
