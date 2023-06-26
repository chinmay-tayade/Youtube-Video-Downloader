package com.chinmay.tayade.mp3downloader.Screens

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chaquo.python.Python
import com.chinmay.tayade.mp3downloader.Fragments.*
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.Utility.UtilityFunction
import kotlinx.coroutines.*


class DownloadingScreen : AppCompatActivity() {

    private lateinit var apiKey :String
    private  var likes :String = ""
    private  var views :String = ""
    private  var tittle :String = ""
    private  var thumbnailUrl:String =""
    private var currentFragmentState =0



    companion object{

        public const val location_Uri:String =""
        public const val  youtube_link :String = ""
    }
    override fun onCreate(savedInstanceState: Bundle?) {



        val coroutineScope: CoroutineScope = MainScope()
        var link = intent.getStringExtra("youtube_link").toString()
        var location_Uri = intent.getStringExtra("location_uri").toString()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloading_screen)


        runBlocking {

           var result = async {   callPythonFunction(link)}

            result.await()

            var thumbnail :ImageView = findViewById(R.id.thumbnail)
            var name_of_video :TextView = findViewById(R.id.name_of_video)
            var viewCounts :TextView = findViewById(R.id.view_counts)
            var likesCounts :TextView = findViewById(R.id.like_counts)
            var frameLayout:FrameLayout = findViewById(R.id.changing_frame)


            UtilityFunction().loadYouTubeThumbnail(thumbnailUrl,thumbnail)
            name_of_video.text = tittle.toString()
            viewCounts.text = "$views Views"
            likesCounts.text = "$likes Likes "




            coroutineScope.launch(Dispatchers.IO) {
                val py = Python.getInstance()
                val pyObj = py.getModule("video_downloader")
                pyObj.callAttr("download_video", link, location_Uri)
            }

            switchFragment()

        }
    }


    private suspend fun callPythonFunction(videoUrl: String) {
        val python = Python.getInstance()
        val videoInfoModule = python.getModule("video_downloader")
        val result = videoInfoModule.callAttr("get_video_info", videoUrl)

        val resultString = result.toString()

        val splitResult = resultString.split(",")
        val regexPattern = "'(.*?)'".toRegex()
        val matchResult = regexPattern.find(splitResult[1])

         tittle = splitResult[0].removePrefix("('").removeSuffix("'")
         thumbnailUrl = matchResult?.value?.trim('\'').toString().trim()
         var views = splitResult[2].replace(Regex("\\D"), "").toString()
        var  likes = splitResult[3].replace(Regex("\\D"), "").toString()

        this.likes = UtilityFunction().formatNumberAbbreviated(likes.toLong())
        this.views = UtilityFunction().formatNumberAbbreviated(views.toLong())





        println("Title: $tittle")
        println("Thumbnail: $thumbnailUrl")
        println("Views: $views")
        println("Likes: $likes")
    }

    private fun switchFragment() {
        val newFragment: Fragment
        when (currentFragmentState) {
            1 -> newFragment = Fragment1()
            2 -> newFragment = Fragment2()
            3 -> newFragment = Fragment3()
            4 -> newFragment = Fragment4()
            5 -> newFragment = Fragment5()
            else -> newFragment = Fragment1() // Default to Fragment1
        }



        // Replace the current fragment with the new fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.changing_frame, newFragment)
            .commit()
    }

}