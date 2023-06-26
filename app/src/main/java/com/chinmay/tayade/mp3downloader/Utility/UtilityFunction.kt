package com.chinmay.tayade.mp3downloader.Utility
import android.content.Context
import android.net.Uri
import java.net.URL
import android.util.Patterns
import android.widget.ImageView
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.*
import com.squareup.picasso.Picasso
import java.util.regex.Pattern

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

    internal fun formatNumberAbbreviated(number: Long): String {
        if (number < 1000) {
            return number.toString()
        }

        val units = arrayOf("K", "M", "B", "T") // Add more units as needed
        val suffixIndex = (Math.log10(number.toDouble()) / 3).toInt()

        val abbreviatedNumber = number / Math.pow(1000.0, suffixIndex.toDouble())
        val formattedNumber = "%.2f".format(abbreviatedNumber)

        return formattedNumber + units[suffixIndex - 1]
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



    fun loadYouTubeThumbnail(url: String, imageView: ImageView) {
        Picasso.get()
            .load(url)
            .into(imageView)
    }




}