/*
 * Copyright (C) 2008-2023, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android.screens.me

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.juick.R
import com.juick.android.ProfileData
import com.juick.android.Status
import com.juick.android.screens.blog.BlogFragmentArgs
import com.juick.databinding.FragmentMeBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.launch

/**
 * Created by alx on 13.12.16.
 */
class MeFragment : Fragment(R.layout.fragment_me) {
    private val binding by viewBinding(FragmentMeBinding::bind)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            val navController = Navigation.findNavController(view)
            val args = BlogFragmentArgs.Builder(binding.username.text as String)
                .build()
            navController.navigate(R.id.blog, args.toBundle())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                ProfileData.userProfile.collect {
                    if (it.status == Status.SUCCESS) {
                        val user = it.data!!
                        val avatarUrl: String = user.avatar
                        binding.username.text = user.uname
                        binding.premiumImageView.visibility = if (user.premium) View.VISIBLE else View.GONE
                        Glide.with(requireActivity())
                            .asBitmap()
                            .load(avatarUrl)
                            .placeholder(R.drawable.av_96)
                            .into(binding.profileMeImage)
                    }
                }
            }
        }
    }
}