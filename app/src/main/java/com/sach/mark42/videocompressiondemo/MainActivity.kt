package com.sach.mark42.videocompressiondemo

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.sach.mark42.videocompressor.CompressionListener
import com.sach.mark42.videocompressor.Compressor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    var isPlaying = false
    var current = 0
    var duration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        select.setOnClickListener {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Video"),
                124
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 124 && resultCode == Activity.RESULT_OK) {

            val selectedImageUri = data?.data
            val videoPath = getPath(selectedImageUri!!)

            compressVideo(videoPath)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(uri: Uri): String {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            return if (cursor != null) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                cursor.moveToFirst()
                cursor.getString(columnIndex)

            } else ""
        }finally {
            cursor?.close()
        }
    }

    private fun setVideo(video: String) {
        val observeDuration = MutableLiveData<Int>()
        var isObserved = false
        val uri = Uri.fromFile(File(video))
        val videoView = findViewById<VideoView>(R.id.videoView)
        val playButton = findViewById<ImageView>(R.id.playButton)
        val currentTimer = findViewById<TextView>(R.id.currentTimer)
        val durationTimer = findViewById<TextView>(R.id.durationTimer)
        val videoProgressbar = findViewById<ProgressBar>(R.id.videoProgressBar)
        val bufferProgress = findViewById<ProgressBar>(R.id.progressBar)

        videoView.setVideoURI(uri)
        videoView.requestFocus()
        videoView.setOnInfoListener { mediaPlayer, i, _ ->
            if (mediaPlayer.isLooping || i == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                bufferProgress.visibility = View.VISIBLE
            } else if (mediaPlayer.isPlaying || i == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                bufferProgress.visibility = View.GONE
            }
            return@setOnInfoListener false
        }

        videoView.setOnPreparedListener {
            duration = it.duration/1000
            if (!isObserved) {
                isObserved = true
                observeDuration.postValue(duration)
            }
            val durationString = String.format("%02d:%02d", duration / 60, duration % 60)
            durationTimer.text = durationString

            val observeTimer = MutableLiveData<String>()
            observeTimer.observe(this, Observer {str ->
                currentTimer.text = str
            })

            GlobalScope.launch (Dispatchers.IO) {
                while (videoProgressbar.progress <= 100) {
                    if (isPlaying) {
                        current = videoView.currentPosition / 1000
                        val currentPercent = current * 100 / duration
                        val currentDuration = String.format("%02d:%02d", current / 60, current % 60)
                        videoProgressbar.progress = currentPercent
                        observeTimer.postValue(currentDuration)
                    }
                }
            }
        }

        observeDuration.observe(this, Observer {dur ->
            /*videos.map {
                FeedVideo(localUrl = it, videoLength = dur)
                    .apply {
                        pushToLocalStorage(this@VideoActivity)
                    }
            }*/
        })

        videoView.start()
        isPlaying = true
        playButton.setImageResource(android.R.drawable.ic_media_pause)

        playButton.setOnClickListener {
            if (isPlaying) {
                videoView.pause()
                isPlaying = false
                playButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                videoView.start()
                isPlaying = true
                playButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun compressVideo(videoPath: String) {
        val file = File(videoPath)
        /*val tempFilePath = "${filesDir.absolutePath}/Compress/videos/temp_video"
        val tempFile = File(tempFilePath)
        if (!file.exists())
            file.mkdirs()*/

        val downloadsPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val desFile = File(downloadsPath, "${System.currentTimeMillis()}_${file.name}")
        if (desFile.exists()) {
            desFile.delete()
            try {
                desFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        Log.v("TestVideo", "size: ${getFileSize(file.length())}")
        Log.v("TestVideo", "size: ${(file.length() / 1024)}")
        //Log.v("TestVideo", "tempFile: $tempFile")
        val progressBar = ProgressDialog.show(this, "", "Please wait")
        Compressor(this).compress(videoPath, desFile.path, object : CompressionListener{

            override fun onSuccess() {
                progressBar.dismiss()
                Log.v("TestVideo", "newSize: ${getFileSize(desFile.length())}")
                Log.v("TestVideo", "newSize: ${(desFile.length() / 1024)}")
                setVideo(desFile.path)
            }

            override fun onFailure() {
                progressBar.dismiss()
                Log.v("TestVideo", "Failure")
            }

            override fun onProgress(percent: Float) {
                Log.e("Test", "Progress")
            }

            override fun onCancelled() {
                Log.v("TestVideo", "Cancel")
            }
        })
    }

    fun getFileSize(size: Long): String {
        if (size <= 0)
            return "0"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(
            size / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }
}

