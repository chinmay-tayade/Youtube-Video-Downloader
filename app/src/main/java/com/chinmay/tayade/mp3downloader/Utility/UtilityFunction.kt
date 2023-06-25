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

       private val YT_DLP_EXECUTABLE_NAME = "yt-dlp"
       private val YT_DLP_SCRIPT_NAME = "yt-dlp.sh"
       private const val REQUEST_READ_PHONE_STATE_PERMISSION = 1
       private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 2
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

    internal fun copyYtDlpExecutableToInternalStorage(context: Context,activity:Activity) {

//        if(checkAndRequestPermissions(context)){
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            // Add more permissions as needed
        )

        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            val result = ContextCompat.checkSelfPermission(activity, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), 1)
        }

        val assetManager = context.assets
        val inputStream = assetManager.open("yt-dlp")

        val outputFile = File(context.filesDir, "yt-dlp")
        val outputStream = FileOutputStream(outputFile)
        outputFile.setExecutable(true)
        inputStream.copyTo(outputStream)
        outputFile.setExecutable(true)
        inputStream.close()
        outputFile.setExecutable(true)
        outputStream.close()
        outputFile.setExecutable(true)
        outputFile.setReadable(true, false); //Sets everyone's permissions to readable
        outputFile.setWritable(true, false); //Sets everyone's permissions to writable
        outputFile.setExecutable(true, false); //Sets everyone's permissions to executable



        val command = "chmod +x ${outputFile.absolutePath}"

        try {
            Runtime.getRuntime().exec(command)
        } catch (e: IOException) {
            e.printStackTrace()
        }
// Execute the yt-dlp file
        try {
            val processBuilder = ProcessBuilder(outputFile.absolutePath)
            processBuilder.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }


//            return true
//        }else{
//
//            Toast.makeText(context,"Permission not granted",Toast.LENGTH_SHORT)
//
//        //    return false
//        }
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

//
//    fun checkAndRequestPermissions(context: Context): Boolean {
//        var permissionsGranted = true
//
//        val readPhoneStatePermission = Manifest.permission.READ_PHONE_STATE
//        if (ContextCompat.checkSelfPermission(context, readPhoneStatePermission)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                context as AppCompatActivity,
//                arrayOf(readPhoneStatePermission),
//                REQUEST_READ_PHONE_STATE_PERMISSION
//            )
//            permissionsGranted = false
//        }
//
//        val writeExternalStoragePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
//        if (ContextCompat.checkSelfPermission(context, writeExternalStoragePermission)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                context as AppCompatActivity,
//                arrayOf(writeExternalStoragePermission),
//                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
//            )
//            permissionsGranted = false
//        }
//
//        // Add more checks and permission requests for other permissions as needed
//
//        return permissionsGranted
//    }
}