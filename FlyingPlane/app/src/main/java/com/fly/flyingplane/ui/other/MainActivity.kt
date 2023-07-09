package com.fly.flyingplane.ui.other

import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.viewModels
import com.fly.flyingplane.R
import com.fly.flyingplane.core.MusicController

class MainActivity : AppCompatActivity(), MusicController {
    private lateinit var mediaPlayer: MediaPlayer
    private val sharedPrefs by lazy {
        this.getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
    }
    private var currentSec = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            currentSec = savedInstanceState.getInt("SEC")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("SEC", mediaPlayer.currentPosition)
    }

    override fun onResume() {
        super.onResume()
        startMusic()
    }

    override fun onPause() {
        super.onPause()
        pauseMusic()
    }

    override fun startMusic() {
        if (sharedPrefs.getBoolean("MUSIC", true)) {
            mediaPlayer = MediaPlayer.create(this, R.raw.bg_music)
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(50f, 50f)
            mediaPlayer.seekTo(currentSec)
            mediaPlayer.start()
        }
    }

    override fun pauseMusic() {
        currentSec = mediaPlayer.currentPosition
        mediaPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}