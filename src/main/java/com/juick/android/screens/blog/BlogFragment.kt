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
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.R
import com.juick.android.Uris
import com.juick.android.Utils.getMimeTypeFor
import com.juick.android.Utils.isImageTypeAllowed
import com.juick.android.screens.FeedAdapter
import com.juick.android.screens.FeedFragment
import com.juick.android.widget.CropBottomSheet
import com.juick.android.widget.util.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class BlogFragment : FeedFragment() {
    private val passedUname: String?
        get() = arguments?.getString("uname")

    private val isOwnBlog: Boolean
        get() = passedUname.isNullOrBlank() || passedUname == account.profile.value?.name

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showCropSheet(it) }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { showCropSheet(it) }
    }
    private var cameraUri: Uri? = null

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
        binding.profileHeaderSubtitle.text = getString(R.string.blog)

        if (isOwnBlog) {
            binding.profileHeaderEdit.isVisible = true
            binding.profileHeaderSettings.isVisible = false
            binding.profileHeaderUsername.text = account.profile.value?.name ?: passedUname.orEmpty()
            val avatarUrl = account.profile.value?.avatar
            if (!avatarUrl.isNullOrEmpty()) {
                binding.profileHeaderAvatar.load(avatarUrl, false, false)
            } else {
                binding.profileHeaderAvatar.setImageResource(R.drawable.av_96)
            }
            binding.profileHeaderEdit.setOnClickListener {
                showImageSourcePicker()
            }
        } else {
            binding.profileHeaderEdit.isVisible = false
            binding.profileHeaderSettings.isVisible = true
            binding.profileHeaderUsername.text = passedUname.orEmpty()
            binding.profileHeaderAvatar.setImageResource(R.drawable.av_96)
        }
        binding.profileHeaderSettings.setOnClickListener { v ->
            showHeaderMenu(v)
        }
    }

    private fun showImageSourcePicker() {
        AlertDialog.Builder(requireContext())
            .setItems(R.array.image_source_options) { _, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> launchCamera()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val file = File(requireContext().filesDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
        cameraLauncher.launch(cameraUri!!)
    }

    private fun showCropSheet(uri: Uri) {
        val sheet = CropBottomSheet.newInstance(uri)
        sheet.onCropResult = { croppedUri -> uploadAvatar(croppedUri) }
        sheet.show(childFragmentManager, CropBottomSheet.TAG)
    }

    private fun uploadAvatar(uri: Uri?) {
        if (uri == null) return
        val mime = getMimeTypeFor(requireContext(), uri)
        if (mime == null || !isImageTypeAllowed(mime)) {
            Toast.makeText(activity, R.string.wrong_image_format, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    App.instance.uploadAvatar(uri, mime)
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Avatar error: " + e.message, Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }
            }
            account.refresh()
        }
    }

    private fun showHeaderMenu(view: View) {
        val userName = passedUname ?: return
        if (isOwnBlog) return

        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.setForceShowIcon(true)
        popupMenu.menu.add(
            Menu.NONE, MENU_ACTION_SUBSCRIBE, Menu.NONE,
            getString(R.string.Subscribe_to) + " @" + userName
        )
        val me = account.profile.value
        if (me != null && (me.premium || me.admin)) {
            if (me.vip.any { it.name == userName }) {
                popupMenu.menu.add(
                    Menu.NONE, MENU_ACTION_REMOVE_FROM_VIP, Menu.NONE,
                    getString(R.string.remove_from_vip)
                )
            } else {
                popupMenu.menu.add(
                    Menu.NONE, MENU_ACTION_ADD_TO_VIP, Menu.NONE,
                    getString(R.string.add_to_vip)
                )
            }
        }
        if (me != null && me.ignored.any { it.name == userName }) {
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_REMOVE_FROM_IGNORELIST, Menu.NONE,
                getString(R.string.remove_from_ignore_list) + " @" + userName
            )
        } else {
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_ADD_TO_IGNORELIST, Menu.NONE,
                getString(R.string.add_to_ignore_list) + " @" + userName
            )
        }
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                MENU_ACTION_SUBSCRIBE -> {
                    processCommand("S @${userName}")
                    true
                }
                MENU_ACTION_ADD_TO_IGNORELIST -> {
                    processCommand("BL @${userName}")
                    true
                }
                MENU_ACTION_REMOVE_FROM_IGNORELIST -> {
                    processCommand("BL @${userName}")
                    true
                }
                MENU_ACTION_ADD_TO_VIP -> {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { App.instance.api.toggleVIP(userName) }
                        account.refresh()
                    }
                    true
                }
                MENU_ACTION_REMOVE_FROM_VIP -> {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { App.instance.api.toggleVIP(userName) }
                        account.refresh()
                    }
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun processCommand(command: String) {
        lifecycleScope.launch {
            App.instance.api.newPost(
                command.toRequestBody("text/plain".toMediaTypeOrNull()),
                null
            )
        }
    }

    companion object {
        private const val MENU_ACTION_SUBSCRIBE = 3
        private const val MENU_ACTION_ADD_TO_VIP = 7
        private const val MENU_ACTION_REMOVE_FROM_VIP = 8
        private const val MENU_ACTION_ADD_TO_IGNORELIST = 9
        private const val MENU_ACTION_REMOVE_FROM_IGNORELIST = 10
    }
}
