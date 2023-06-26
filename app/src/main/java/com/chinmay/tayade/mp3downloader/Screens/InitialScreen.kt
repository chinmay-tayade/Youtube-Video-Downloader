package com.chinmay.tayade.mp3downloader.Screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.R.layout.activity_initial_screen
import com.chinmay.tayade.mp3downloader.Utility.UtilityFunction
import java.io.File


class InitialScreen : AppCompatActivity() {


    private var documentTreeUri: String = ""
    private lateinit var buttonDownload: AppCompatButton
    private lateinit var filePathView: RelativeLayout
    private lateinit var clearText: ImageView
    private lateinit var editTextLink: EditText
    private lateinit var idPasteLink: ImageView
   private  lateinit var pathView: TextView
   private  lateinit var progressButton: LinearLayout
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        setContentView(activity_initial_screen)
        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val py = Python.getInstance()
        val module = py.getModule("video_downloader")

        val downloadDir: File? = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val downloadPath: String = downloadDir!!.absolutePath
        checkReadWritePermission()


        buttonDownload = findViewById(R.id.button_download)
        filePathView = findViewById(R.id.filePathView)
        clearText = findViewById(R.id.clear_text)
        editTextLink = findViewById(R.id.editTextLink)
        idPasteLink = findViewById(R.id.pasteLink)
        pathView= findViewById(R.id.path)
        progressButton = findViewById(R.id.progressbutton)



        idPasteLink.setOnClickListener {

            pasteClipboard(editTextLink)
        }


        filePathView.setOnClickListener {
            folderPickerLauncher.launch(null)
        }

        clearText.setOnClickListener {
            editTextLink.setText("")
        }

        editTextLink.setOnClickListener {
            pasteClipboard(editTextLink)
        }

        buttonDownload.setOnClickListener {

            onButtonClick()

        }


    }

    private fun pasteClipboard(editTextLink: EditText) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val item = clipData.getItemAt(0)
            var pasteData = item.text.toString()

            editTextLink.setText(pasteData)
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    fun onButtonClick() {

       if(documentTreeUri.toString().isNullOrEmpty()||editTextLink.text.isNullOrBlank()||documentTreeUri.toString()=="null"||documentTreeUri.toString()==""){

           if(documentTreeUri.toString().isNullOrBlank()||documentTreeUri.toString()=="null"||documentTreeUri.toString()==""){

               Toast.makeText(this,"please select file destination",Toast.LENGTH_SHORT).show()
           }else if(editTextLink.text.isNullOrBlank()){

               Toast.makeText(this,"please enter a url",Toast.LENGTH_SHORT).show()
           }
       }else{
           if(UtilityFunction().isUrl(editTextLink.text.toString())){

               pathView.setTextColor(resources.getColor(R.color.grey_text_loading))
               editTextLink.setTextColor(resources.getColor(R.color.grey_text_loading))

               buttonDownload.visibility = View.GONE
               progressButton.visibility = View.VISIBLE

               var link = editTextLink.text.toString()

               val handler = Handler()
               handler.postDelayed(Runnable {
                   var downloadIntent = Intent(this,DownloadingScreen::class.java)
               downloadIntent.putExtra("youtube_link" ,link)
               downloadIntent.putExtra("location_uri" ,documentTreeUri)
                   startActivity(downloadIntent)
                   this.finish()
               }, 5000)

           }else{

               Toast.makeText(this,"malformed url",Toast.LENGTH_SHORT).show()
           }
       }

    }


    private val READ_WRITE_PERMISSION_REQUEST_CODE = 1

    @RequiresApi(Build.VERSION_CODES.R)
    fun checkReadWritePermission() {
        if (Environment.isExternalStorageManager()) {
            // Permission already granted
            // Perform your logic here
        } else {
            // Permission not granted, request it
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, READ_WRITE_PERMISSION_REQUEST_CODE)
        }
    }



    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Folder selected
            val folderPath = uri.path ?: ""
          //  documentTreeUri = uri
           // Log.e("DocumentTree", uri.toString())

            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
             val path = directory.absolutePath

            documentTreeUri = path
            Log.e("path :",path)


            pathView.text = path.toString().substringAfterLast('/')
                .replace("%3A", ":")
        } else {

            Toast.makeText(this,"select a destination",Toast.LENGTH_LONG).show()
        }
    }




}