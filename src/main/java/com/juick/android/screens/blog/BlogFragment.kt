/*
 * Copyright (C) 2008-2026, Juick
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

package com.juick.android.screens.blog

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.juick.android.Uris
import com.juick.android.screens.FeedAdapter
import com.juick.android.screens.FeedFragment
import com.juick.android.widget.util.load
import com.juick.api.model.Post

class BlogFragment : FeedFragment() {
    private val passedUname: String?
        get() = arguments?.getString("uname")

    private val isOwnBlog: Boolean
        get() = passedUname.isNullOrBlank() || passedUname == account.profile.value?.name

    private lateinit var avatarMediaLauncher: ActivityResultLauncher<CropImageContractOptions>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureBlogUrl()
        account.profile.observe(viewLifecycleOwner) {
            ensureBlogUrl()
            bindProfileHeader()
        }
        bindProfileHeader()
    }

    override fun setupHeaderClickListener(adapter: FeedAdapter) {
        // No-op: clicking on post header when already on blog page should not navigate
    }

    private fun ensureBlogUrl() {
        if (vm.apiUrl.value != Uri.EMPTY) return
        resolveBlogName()?.let {
            vm.apiUrl.value = Uris.getUserPostsByName(it)
        }
    }

    private fun resolveBlogName(): String? {
        return passedUname?.takeIf { it.isNotBlank() }
            ?: account.profile.value?.name?.takeIf { it.isNotBlank() }
    }

    private fun bindProfileHeader() {
        binding.profileHeader.isVisible = true
        binding.profileHeaderSubtitle.text = getString(com.juick.R.string.blog)

        if (isOwnBlog) {
            binding.profileHeaderSettings.isVisible = true
            binding.profileHeaderUsername.text = account.profile.value?.name ?: passedUname.orEmpty()
            val avatarUrl = account.profile.value?.avatar
            if (!avatarUrl.isNullOrEmpty()) {
                binding.profileHeaderAvatar.load(avatarUrl, false, false)
            } else {
                binding.profileHeaderAvatar.setImageResource(com.juick.R.drawable.av_96)
            }
        } else {
            binding.profileHeaderSettings.isVisible = false
            binding.profileHeaderUsername.text = passedUname.orEmpty()
            binding.profileHeaderAvatar.setImageResource(com.juick.R.drawable.av_96)
        }
    }
}
