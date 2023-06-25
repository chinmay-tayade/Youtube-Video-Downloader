package com.chinmay.tayade.mp3downloader.Utility
import android.content.Context
import android.net.Uri
import java.net.URL
import android.util.Patterns
import java.io.*
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class UtilityFunction {

   companion object{
   }

    internal fun isUrl(input: String): Boolean {
        val urlPattern = Patterns.WEB_URL
        if (urlPattern.matcher(input).matches()) {
            try {
                val url = URL(input)
                url.openConnection()
                return true
            } catch (e: Exception) {
                return false
            }
        }
      return false
    }


    internal fun downloadVideo(context: Context, videoUrl: String, targetUri: Uri) {
        val ytDlpScript = File(context.filesDir, "yt-dlp")
        ytDlpScript.setExecutable(true)

        val command = "./${ytDlpScript.absolutePath} -o ${targetUri.path} $videoUrl"
        val process = Runtime.getRuntime().exec(command)

        val exitCode = process.waitFor()
        if (exitCode == 0) {
            // Download completed successfully
        } else {
            // Download failed
        }
    }

}