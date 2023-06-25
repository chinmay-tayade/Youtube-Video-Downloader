package com.chinmay.tayade.mp3downloader.Screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
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


    private var documentTreeUri: Uri? = null
    private lateinit var buttonDownload: AppCompatButton
    private lateinit var filePathView: RelativeLayout
    private lateinit var clearText: ImageView
    private lateinit var editTextLink: EditText
    private lateinit var idPasteLink: ImageView
   private  lateinit var pathView: TextView

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


        buttonDownload = findViewById(R.id.button_download)
        filePathView = findViewById(R.id.filePathView)
        clearText = findViewById(R.id.clear_text)
        editTextLink = findViewById(R.id.editTextLink)
        idPasteLink = findViewById(R.id.pasteLink)
        pathView= findViewById(R.id.path)


        idPasteLink.setOnClickListener {

            pasteClipboard(editTextLink)
        }


        filePathView.setOnClickListener {
            openDocumentTreeLauncher.launch(null)
        }

        clearText.setOnClickListener {
            editTextLink.setText("")
        }

        editTextLink.setOnClickListener {
            pasteClipboard(editTextLink)
        }

        buttonDownload.setOnClickListener {

            onButtonClick(module)

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

    fun onButtonClick(module: PyObject) {

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
               var viewSwitcher: ViewSwitcher = findViewById(R.id.viewSwitcher)
               viewSwitcher.showNext()

                 // UtilityFunction().downloadVideo(this,editTextLink.text.toString(),documentTreeUri!!)

               val pyFunc = module.callAttr("download_video",
                   editTextLink.text.toString(),documentTreeUri.toString())

           }else{

               Toast.makeText(this,"malformed url",Toast.LENGTH_SHORT).show()
           }
       }




    }

    private val openDocumentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                documentTreeUri = uri
                Log.e("DocumentTree", uri.toString())

                pathView.text = uri.toString().substringAfterLast('/')
                    .replace("%3A", ":")
            }
        }


}