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
package com.juick.android.screens.post

import android.app.ProgressDialog
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juick.App
import com.juick.R
import com.juick.android.Utils.getMimeTypeFor
import com.juick.android.Utils.isImageTypeAllowed
import com.juick.android.screens.home.HomeFragmentDirections
import com.juick.api.model.Post
import com.juick.databinding.FragmentNewPostBinding
import com.juick.util.StringUtils
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by alx on 02.01.17.
 */
class NewPostFragment : Fragment() {
    private var attachmentUri: Uri? = null
    private var attachmentMime: String? = null
    private lateinit var progressDialog: ProgressDialog
    private val progressDialogCancel = BooleanReference(false)

    class BooleanReference(var bool: Boolean)

    private var _model: FragmentNewPostBinding? = null
    private val model get() = _model!!
    private lateinit var attachmentLauncher: ActivityResultLauncher<String>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _model = FragmentNewPostBinding.inflate(inflater, container, false)
        return model.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachmentLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> attachImage(uri) }
        model.buttonTags.setOnClickListener { v: View? ->
            val navController = findNavController(requireView())
            val tagState = navController.currentBackStackEntry
                ?.savedStateHandle as SavedStateHandle
            val tagData = tagState.getLiveData<String>("tag")
            tagData.observe(viewLifecycleOwner) { tag: String? ->
                tagState.remove<Any>("tag")
                applyTag(tag)
            }
            val action = NewPostFragmentDirections.actionNewPostToTags()
            findNavController(v!!).navigate(action)
        }
        model.buttonAttachment.setOnClickListener { v: View? ->
            if (attachmentUri == null) {
                try {
                    attachmentLauncher.launch("image/*")
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
        model.buttonSend.setOnClickListener { v: View? ->
            try {
                sendMessage()
            } catch (e: FileNotFoundException) {
                Toast.makeText(activity, "Attachment error: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
        val args = NewPostFragmentArgs.fromBundle(requireArguments())
        if (!StringUtils.defaultString(args.text).isEmpty()) {
            applyTag(args.text)
        }
        if (!StringUtils.defaultString(args.uri).isEmpty()) {
            attachImage(Uri.parse(args.uri))
        }
        model.editMessage.requestFocus()
    }

    private fun setFormEnabled(state: Boolean) {
        model.editMessage.isEnabled = state
        model.buttonTags.isEnabled = state
        model.buttonAttachment.isEnabled = state
        model.buttonSend.isEnabled = state
        //setSupportProgressBarIndeterminateVisibility(state ? Boolean.FALSE : Boolean.TRUE);
    }

    @Throws(FileNotFoundException::class)
    private fun sendMessage() {
        val msg = model.editMessage.text.toString()
        if (msg.length < 3 && attachmentUri == null) {
            //Toast.makeText(getBaseActivity(), R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return
        }
        setFormEnabled(false)
        if (attachmentUri != null) {
            progressDialog = ProgressDialog(activity)
            progressDialogCancel.bool = false
            progressDialog.setOnCancelListener {
                progressDialogCancel.bool = true
            }
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.max = 0
            progressDialog.show()
            App.instance.setOnProgressListener { progressPercentage: Long ->
                if (progressDialog.max < progressPercentage) {
                    progressDialog.max = progressPercentage.toInt()
                } else {
                    progressDialog.progress = progressPercentage.toInt()
                }
            }
        }
        lifecycleScope.launch {
            App.instance.sendMessage(msg, attachmentUri, attachmentMime) { newMessage: Post? ->
                progressDialog.dismiss()
                if (newMessage == null) {
                    Toast.makeText(activity, R.string.Error, Toast.LENGTH_LONG).show()
                } else {
                    val mid = newMessage.mid
                    val discoverAction =
                        HomeFragmentDirections.actionDiscoverFragmentToThreadFragment()
                    discoverAction.mid = mid
                    findNavController(requireView()).navigate(discoverAction)
                }
            }
        }
    }

    private fun attachImage(uri: Uri?) {
        if (uri != null) {
            val mime = getMimeTypeFor(requireContext(), uri)
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

    private fun applyTag(tag: String?) {
        model.editMessage.setText("*$tag ${model.editMessage.text}")
        val textLength = model.editMessage.text.length
        model.editMessage.setSelection(textLength, textLength)
        model.editMessage.requestFocus()
    }

    override fun onDestroyView() {
        _model = null
        super.onDestroyView()
    }
}