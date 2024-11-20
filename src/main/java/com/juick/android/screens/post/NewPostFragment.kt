/*
 * Copyright (C) 2008-2024, Juick
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

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.juick.App
import com.juick.R
import com.juick.android.Utils.getMimeTypeFor
import com.juick.android.Utils.isImageTypeAllowed
import com.juick.api.model.PostResponse
import com.juick.databinding.FragmentNewPostBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
    private lateinit var attachmentMediaLauncher: ActivityResultLauncher<CropImageContractOptions>
    private val messagePosted = MutableStateFlow<Result<PostResponse>?>(null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachmentLegacyLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachImage(uri) }
        attachmentMediaLauncher = registerForActivityResult(CropImageContract()) {
                result ->
            if (result.isSuccessful) {
                // Use the returned uri.
                val uriContent = result.uriContent
                attachImage(uriContent)
            } else {
                // An error occurred.
                val exception = result.error
                Toast.makeText(
                    activity,
                    exception?.message ?: getText(R.string.Error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        model.buttonTags.setOnClickListener {
            val navController = findNavController(this)
            val tagState = navController.currentBackStackEntry
                ?.savedStateHandle as SavedStateHandle
            val tagData = tagState.getLiveData<String>("tag")
            tagData.observe(viewLifecycleOwner) { tag: String? ->
                tagState.remove<Any>("tag")
                applyTag(tag)
            }
            navController.navigate(R.id.tags)
        }
        model.buttonAttachment.setOnClickListener {
            if (attachmentUri == null) {
                try {
                    if (Build.VERSION.SDK_INT >= 21) {
                        attachmentMediaLauncher.launch(
                            CropImageContractOptions(
                                uri = null,
                                cropImageOptions = CropImageOptions(
                                    imageSourceIncludeCamera = true,
                                    imageSourceIncludeGallery = true
                                ),
                            ),
                        )
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
        val text = arguments?.getString("text") ?: ""
        if (text.isNotEmpty()) {
            model.editMessage.setText(text)
        }
        val uri = arguments?.getString("uri") ?: ""
        if (uri.isNotEmpty()) {
            attachImage(Uri.parse(uri))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                messagePosted.collect { response ->
                    when (response) {
                        null -> {}
                        else -> {
                            response.fold(
                                onSuccess = { post ->
                                    model.progressBar.visibility = View.GONE
                                    Toast.makeText(activity, post.text, Toast.LENGTH_LONG).show()
                                    post.newMessage?.let {
                                        val navController = findNavController(this@NewPostFragment)
                                        navController.popBackStack(R.id.new_post, true)
                                        val args = Bundle()
                                        args.putInt("mid", it.mid)
                                        navController.navigate(R.id.thread, args)
                                    }
                                    setFormEnabled(true)
                                },
                                onFailure = {
                                    Toast.makeText (context, requireContext().getString(R.string.network_error), Toast.LENGTH_LONG).show()
                                }
                            )
                            messagePosted.update { null }
                        }

                    }
                }
            }
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
        App.instance.sendMessage(lifecycleScope, messagePosted, msg, attachmentUri, attachmentMime)
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