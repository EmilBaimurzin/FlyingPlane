package com.fly.flyingplane.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.fly.flyingplane.core.soundClickListener
import com.fly.flyingplane.databinding.FragmentSettingsBinding
import com.fly.flyingplane.ui.other.MainActivity
import com.fly.flyingplane.ui.other.ViewBindingFragment

class FragmentSettings :
    ViewBindingFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {
    private val sharedPrefs by lazy {
        requireActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnShot.text = "Shot sound: " + if (getShotState()) "ON" else "OFF"
            btnShot.soundClickListener {
                changeShotState()
            }

            soundBtn.text = "Sound effects: " + if (getEffectsState()) "ON" else "OFF"
            soundBtn.soundClickListener {
                changeEffectsState()
            }

            btnMusic.text = "Music: " + if (getMusicState()) "ON" else "OFF"
            btnMusic.soundClickListener {
                changeMusicState()
            }

            btnback.soundClickListener {
                findNavController().popBackStack()
            }
        }
    }

    private fun changeShotState() {
        sharedPrefs.edit().putBoolean("SHOT", !getShotState()).apply()
        binding.btnShot.text = "Shot sound: " + if (getShotState()) "ON" else "OFF"
    }

    private fun getShotState(): Boolean = sharedPrefs.getBoolean("SHOT", true)

    private fun changeEffectsState() {
        sharedPrefs.edit().putBoolean("SFX", !getEffectsState()).apply()
        binding.soundBtn.text = "Sound effects: " + if (getEffectsState()) "ON" else "OFF"
    }

    private fun getEffectsState(): Boolean = sharedPrefs.getBoolean("SFX", true)

    private fun changeMusicState() {
        sharedPrefs.edit().putBoolean("MUSIC", !getMusicState()).apply()
        binding.btnMusic.text = "Music: " + if (getMusicState()) "ON" else "OFF"

        if (getMusicState()) {
            (requireActivity() as MainActivity).startMusic()
        } else {
            (requireActivity() as MainActivity).pauseMusic()
        }
    }

    private fun getMusicState(): Boolean = sharedPrefs.getBoolean("MUSIC", true)
}