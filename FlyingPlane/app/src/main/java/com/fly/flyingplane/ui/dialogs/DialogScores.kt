package com.fly.flyingplane.ui.dialogs

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fly.flyingplane.R
import com.fly.flyingplane.core.soundClickListener
import com.fly.flyingplane.databinding.DialogScoresBinding
import com.fly.flyingplane.ui.other.ViewBindingDialog

class DialogScores : ViewBindingDialog<DialogScoresBinding>(DialogScoresBinding::inflate) {
    private val args: DialogScoresArgs by navArgs()
    private var isRecord = false
    private var scores = 0L
    private val sharedPrefs by lazy {
        requireActivity().getSharedPreferences("SHARED_PREFS", MODE_PRIVATE)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Dialog_No_Border)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.setCancelable(false)
        dialog!!.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                findNavController().popBackStack(R.id.fragmentMenu, false, false)
                true
            } else {
                false
            }
        }
        isRecord = args.isRecord
        scores = args.scores
        binding.scoresTextView.text = scores.toString()
        binding.bestScoresTextView.text = sharedPrefs.getLong("RECORD", 0).toString()
        binding.container.setBackgroundResource(if (isRecord) R.drawable.bg_dialog_new_record else R.drawable.bg_dialog_game_over)
        binding.scoreTextView.text =
            if (isRecord) resources.getString(R.string.new_best_score) else resources.getString(R.string.plane_crashed)

        binding.buttonCancel.soundClickListener {
            findNavController().popBackStack(R.id.fragmentMenu, false, false)
        }

        binding.buttonOk.soundClickListener {
            findNavController().popBackStack(R.id.fragmentMenu, false, false)
            findNavController().navigate(R.id.action_fragmentMenu_to_fragmentGame)
        }
    }
}