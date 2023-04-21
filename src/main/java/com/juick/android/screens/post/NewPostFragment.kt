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
package com.juick.android.screens.post

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.juick.App
import com.juick.R
import com.juick.android.Utils.getMimeTypeFor
import com.juick.android.Utils.isImageTypeAllowed
import com.juick.databinding.FragmentNewPostBinding
import com.juick.util.StringUtils
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by alx on 02.01.17.
 */
class NewPostFragment : Fragment(R.layout.fragment_new_post) {
    private var attachmentUri: Uri? = null
    private var attachmentMime: String? = null

    private val model by viewBinding(FragmentNewPostBinding::bind)
    private lateinit var attachmentLegacyLauncher: ActivityResultLauncher<String>
    private lateinit var attachmentMediaLauncher: ActivityResultLauncher<Intent>
    private val args by navArgs<NewPostFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachmentLegacyLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachImage(uri) }
        attachmentMediaLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val fileUri = data?.data!!

                    attachImage(fileUri)
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(activity, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
            }
        model.buttonTags.setOnClickListener {
            val navController = findNavController(requireView())
            val tagState = navController.currentBackStackEntry
                ?.savedStateHandle as SavedStateHandle
            val tagData = tagState.getLiveData<String>("tag")
            tagData.observe(viewLifecycleOwner) { tag: String? ->
                tagState.remove<Any>("tag")
                applyTag(tag)
            }
            val action = NewPostFragmentDirections.actionNewPostToTags()
            findNavController(view).navigate(action)
        }
        model.buttonAttachment.setOnClickListener {
            if (attachmentUri == null) {
                try {
                    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable()) {
                        ImagePicker.with(this)
                            .crop()
                            .createIntent { intent ->
                                attachmentMediaLauncher.launch(intent)
                            }
                    } else {
                        attachmentLegacyLauncher.launch("image/*")
                    }
                } catch (e: Exception) {
                    Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                }
            } else {
                attachmentUri = null
                attachmentMime = null
                model.buttonAttachment.isSelected = false
                Glide.with(requireContext())
                    .clear(model.imagePreview)
            }
        }
        model.buttonSend.setOnClickListener {
            try {
                sendMessage()
            } catch (e: FileNotFoundException) {
                Toast.makeText(activity, "Attachment error: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
        if (StringUtils.defaultString(args.text).isNotEmpty()) {
            applyTag(args.text)
        }
        if (StringUtils.defaultString(args.uri).isNotEmpty()) {
            attachImage(Uri.parse(args.uri))
        }
        model.editMessage.requestFocus()
    }

    private fun setFormEnabled(state: Boolean) {
        model.editMessage.isEnabled = state
        model.buttonTags.isEnabled = state
        model.buttonAttachment.isEnabled = state
        model.buttonSend.isEnabled = state
    }

    @Throws(FileNotFoundException::class)
    private fun sendMessage() {
        val msg = model.editMessage.text.toString()
        if (msg.length < 3 && attachmentUri == null) {
            //Toast.makeText(getBaseActivity(), R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return
        }
        setFormEnabled(false)
        model.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            App.instance.sendMessage(msg, attachmentUri, attachmentMime) { response ->
                model.progressBar.visibility = View.GONE
                Toast.makeText(activity, response.text, Toast.LENGTH_LONG).show()
                response.newMessage?.let {
                    val navController = findNavController(requireView())
                    navController.popBackStack(R.id.new_post, true)
                    val args = Bundle()
                    args.putInt("mid", it.mid)
                    navController.navigate(R.id.thread, args)
                }
                setFormEnabled(true)
            }
        }
    }

    private fun attachImage(uri: Uri?) {
        if (uri != null) {
            getMimeTypeFor(requireContext(), uri)?.let { mime ->
                if (isImageTypeAllowed(mime)) {
                    attachmentUri = uri
                    attachmentMime = mime
                    model.buttonAttachment.isSelected = true
                    try {
                        requireContext().contentResolver.openInputStream(uri).use { bitmapStream ->
                            val image = BitmapFactory.decodeStream(bitmapStream)
                            Glide.with(requireContext())
                                .load(image)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(model.imagePreview)
                        }
                    } catch (e: IOException) {
                        Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, R.string.wrong_image_format, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun applyTag(tag: String?) {
        model.editMessage.setText("*$tag ${model.editMessage.text}")
        val textLength = model.editMessage.text?.length ?: 0
        model.editMessage.setSelection(textLength, textLength)
        model.editMessage.requestFocus()
    }
}