package com.chinmay.tayade.mp3downloader.Screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.Utility.UtilityFunction
import java.io.File

class InitialScreen : AppCompatActivity() {

    private val utils = UtilityFunction()

    /** Absolute path of the folder downloads are written to. Empty until chosen. */
    private var destinationPath: String = ""

    private lateinit var buttonDownload: AppCompatButton
    private lateinit var filePathView: RelativeLayout
    private lateinit var clearText: ImageView
    private lateinit var editTextLink: EditText
    private lateinit var idPasteLink: ImageView
    private lateinit var pathView: TextView
    private lateinit var progressButton: LinearLayout

    private var pendingNavigation: Runnable? = null

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "Select a destination folder", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            // yt-dlp writes through a plain filesystem path, so fall back to the
            // public Downloads directory regardless of the tree that was picked.
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            destinationPath = downloads?.absolutePath ?: getFallbackDownloadDir()
            Log.d(TAG, "Destination path: $destinationPath")
            pathView.text = destinationPath.substringAfterLast('/')
        }

    private val legacyStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial_screen)

        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(applicationContext))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start Python runtime", e)
        }

        buttonDownload = findViewById(R.id.button_download)
        filePathView = findViewById(R.id.filePathView)
        clearText = findViewById(R.id.clear_text)
        editTextLink = findViewById(R.id.editTextLink)
        idPasteLink = findViewById(R.id.pasteLink)
        pathView = findViewById(R.id.path)
        progressButton = findViewById(R.id.progressbutton)

        ensureStoragePermission()

        idPasteLink.setOnClickListener { pasteClipboard() }
        editTextLink.setOnClickListener { pasteClipboard() }
        filePathView.setOnClickListener { launchFolderPicker() }
        clearText.setOnClickListener { editTextLink.setText("") }
        buttonDownload.setOnClickListener { onButtonClick() }
    }

    override fun onDestroy() {
        pendingNavigation?.let { buttonDownload.removeCallbacks(it) }
        pendingNavigation = null
        super.onDestroy()
    }

    private fun launchFolderPicker() {
        try {
            folderPickerLauncher.launch(null)
        } catch (e: Exception) {
            // No system file picker available – fall back to public Downloads.
            destinationPath = getFallbackDownloadDir()
            pathView.text = destinationPath.substringAfterLast('/')
            Toast.makeText(this, "Using Downloads folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip == null || clip.itemCount == 0) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val pasted = clip.getItemAt(0)?.text?.toString().orEmpty()
        if (pasted.isBlank()) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }
        editTextLink.setText(pasted)
        editTextLink.setSelection(pasted.length)
    }

    fun onButtonClick() {
        val link = editTextLink.text?.toString()?.trim().orEmpty()

        if (destinationPath.isBlank()) {
            Toast.makeText(this, "Please select a file destination", Toast.LENGTH_SHORT).show()
            return
        }
        if (link.isBlank()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            return
        }
        if (!utils.isUrl(link)) {
            Toast.makeText(this, "That does not look like a valid URL", Toast.LENGTH_SHORT).show()
            return
        }

        pathView.setTextColor(ContextCompat.getColor(this, R.color.grey_text_loading))
        editTextLink.setTextColor(ContextCompat.getColor(this, R.color.grey_text_loading))
        buttonDownload.visibility = View.GONE
        progressButton.visibility = View.VISIBLE

        val navigate = Runnable {
            if (isFinishing || isDestroyed) return@Runnable
            val downloadIntent = Intent(this, DownloadingScreen::class.java).apply {
                putExtra("youtube_link", link)
                putExtra("location_uri", destinationPath)
            }
            startActivity(downloadIntent)
            finish()
        }
        pendingNavigation = navigate
        buttonDownload.postDelayed(navigate, NAVIGATION_DELAY_MS)
    }

    private fun ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) return
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun getFallbackDownloadDir(): String {
        val external: File? = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return external?.absolutePath ?: File(filesDir, "downloads").apply { mkdirs() }.absolutePath
    }

    companion object {
        private const val TAG = "InitialScreen"
        private const val NAVIGATION_DELAY_MS = 1500L
    }
}
