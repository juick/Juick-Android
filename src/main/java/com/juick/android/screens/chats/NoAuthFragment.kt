/*
 * Copyright (C) 2008-2022, Juick
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
package com.juick.android.screens.chats

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.juick.R
import com.juick.android.ProfileViewModel
import com.juick.android.SignInActivity
import com.juick.databinding.FragmentNoAuthBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding

class NoAuthFragment : Fragment(R.layout.fragment_no_auth) {
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val binding by viewBinding(FragmentNoAuthBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.signInButton.setOnClickListener {
            profileViewModel.signInStatus.value =
                SignInActivity.SignInStatus.SIGN_IN_PROGRESS
        }
    }
}