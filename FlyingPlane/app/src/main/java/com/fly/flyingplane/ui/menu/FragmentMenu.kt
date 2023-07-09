package com.fly.flyingplane.ui.menu

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.fly.flyingplane.R
import com.fly.flyingplane.core.soundClickListener
import com.fly.flyingplane.databinding.FragmentMenuBinding
import com.fly.flyingplane.ui.other.ViewBindingFragment

class FragmentMenu: ViewBindingFragment<FragmentMenuBinding>(FragmentMenuBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStart.soundClickListener {
            findNavController().navigate(R.id.action_fragmentMenu_to_fragmentGame)
        }
        binding.exitButton.soundClickListener {
            requireActivity().finish()
        }

        binding.settingsButton.soundClickListener {
            findNavController().navigate(R.id.action_fragmentMenu_to_fragmentSettings)
        }
    }
}