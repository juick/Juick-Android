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
package com.juick.android.fragment

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.juick.App
import com.juick.R
import com.juick.android.Account
import com.juick.android.JuickMessageMenuListener
import com.juick.android.SignInActivity
import com.juick.android.Utils.getMimeTypeFor
import com.juick.android.Utils.isImageTypeAllowed
import com.juick.android.screens.FeedAdapter
import com.juick.android.service.isAuthenticated
import com.juick.android.widget.CropBottomSheet
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.api.model.isReply
import com.juick.databinding.FragmentThreadBinding
import com.juick.util.StringUtils
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 *
 * @author Ugnich Anton
 */
class ThreadFragment : BottomSheetDialogFragment(R.layout.fragment_thread) {
    private val account by activityViewModels<Account>()
    private val model by viewBinding(FragmentThreadBinding::bind)
    private var rid = 0
    private var attachmentUri: Uri? = null
    private var attachmentMime: String? = null
    private var mid = 0
    private var scrollToEnd = false
    private val messagePosted = MutableStateFlow<Result<PostResponse>?>(null)
    private val apiResponded = MutableStateFlow<Result<Post>?>(null)

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showCropSheet(it) }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { showCropSheet(it) }
    }
    private var cameraUri: Uri? = null

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
        sheet.onCropResult = { croppedUri -> handleSelectedUri(croppedUri) }
        sheet.show(childFragmentManager, CropBottomSheet.TAG)
    }

    private fun handleSelectedUri(uri: Uri?) {
        if (uri != null) {
            attachmentUri = uri
            getMimeTypeFor(requireActivity(), uri)?.let { mime ->
                if (isImageTypeAllowed(mime)) {
                    attachmentMime = mime
                    model.buttonAttachment.isSelected = true
                } else {
                    Toast.makeText(
                        activity,
                        R.string.wrong_image_format,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.threadList.adapter = FeedAdapter(true)
        (dialog as? BottomSheetDialog)?.behavior?.let {
            it.state = BottomSheetBehavior.STATE_EXPANDED
            it.isDraggable = false
        }
        mid = arguments?.getInt("mid") ?: 0
        scrollToEnd = arguments?.getBoolean("scrollToEnd") ?: false
        if (mid == 0) {
            return
        }
        model.buttonSend.setOnClickListener {
            if (!App.instance.isAuthenticated) {
                startActivity(Intent(context, SignInActivity::class.java))
                return@setOnClickListener
            }
            val msg = model.editMessage.text.toString()
            if (msg.length < 3 && attachmentUri == null) {
                Toast.makeText(context, R.string.Enter_a_message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            var msgnum = "#$mid"
            if (rid > 0) {
                msgnum += "/$rid"
            }
            val body = "$msgnum $msg"
            setFormEnabled(false)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    postReply(body)
                } catch (e: FileNotFoundException) {
                    Toast.makeText(
                        context,
                        "Attachment error: " + e.message,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        model.buttonAttachment.setOnClickListener {
            if (attachmentUri == null) {
                showImageSourcePicker()
            } else {
                attachmentUri = null
                attachmentMime = null
                model.buttonAttachment.isSelected = false
            }
        }
        val linearLayoutManager = model.threadList.layoutManager as LinearLayoutManager
        account.profile.observe(viewLifecycleOwner) {
            it?.let { user ->
                val adapter = model.threadList.adapter as FeedAdapter
                adapter.setOnMenuListener(
                    JuickMessageMenuListener(
                        requireActivity(), this, messagePosted, apiResponded, user
                    )
                )
                adapter.setOnItemClickListener { widget: View?, position: Int ->
                    if (widget?.tag == null || widget.tag != "clicked") {
                        val post = adapter.currentList[position]
                        onReply(post?.rid ?: 0, StringUtils.defaultString(post?.text))
                    }
                }
                adapter.setOnScrollListener { _, replyTo, rid ->
                    var pos = 0
                    for (i in adapter.currentList.indices) {
                        val p = adapter.currentList[i]
                        if (p.rid == replyTo) {
                            p.nextRid = replyTo
                            if (p.prevRid == 0) p.prevRid = rid
                            pos = i
                            break
                        }
                    }
                    if (pos != 0) {
                        adapter.notifyItemChanged(pos)
                        model.threadList.scrollToPosition(pos)
                    }
                }
                model.threadList.adapter = adapter
                load()
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        App.instance.messages.collect { messages ->
                            messages.forEach { post ->
                                val adapter = (model.threadList.adapter as FeedAdapter)
                                if (adapter.itemCount > 0) {
                                    if (adapter.currentList[0]?.mid == post.mid && post.isReply()
                                        && !adapter.currentList.contains(post)
                                    ) {
                                        adapter.submitList(adapter.currentList + post)
                                        val lastVisible = linearLayoutManager.findLastVisibleItemPosition()
                                        val total = adapter.currentList.size - 1 - 1
                                        if (lastVisible == total) {
                                            model.threadList.scrollToPosition(post.rid)
                                        }
                                    }
                                }
                            }
                            App.instance.messages.update { listOf() }
                        }
                    }
                }
            }
        }

        model.swipeContainer.isEnabled = false
        model.threadList.visibility = View.GONE
        model.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                messagePosted.collect { response ->
                    when (response) {
                        null -> {}
                        else -> {
                            response.fold(
                                onSuccess = { post ->
                                    model.progressBar.visibility = View.GONE
                                    setFormEnabled (true)
                                    Toast.makeText (context, post.text, Toast.LENGTH_LONG).show()
                                    post.newMessage?.let {
                                        model.textReplyTo.text = ""
                                        model.editMessage.text?.clear()
                                        mid = it.mid
                                        scrollToEnd = true
                                    }
                                    load()
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                apiResponded.collect { response ->
                    when (response) {
                        null -> {}
                        else -> {
                            response.fold(
                                onSuccess = {
                                    load()
                                },
                                onFailure = {
                                    Toast.makeText (context, it.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                            )
                            apiResponded.update { null }
                        }
                    }
                }
            }
        }
    }

    private fun load() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val posts = App.instance.api.thread(mid)
                withContext(Dispatchers.Main) {
                    model.threadList.visibility = View.VISIBLE
                    model.progressBar.visibility = View.GONE
                    (model.threadList.adapter as FeedAdapter).submitList(posts)
                    val lastReply = posts.last()
                    App.instance.api.markRead(lastReply.mid, lastReply.rid)
                    if (scrollToEnd) {
                        model.threadList.scrollToPosition(posts.size - 1)
                    }
                }
            } catch (e: Exception) {
                Log.e("Thread", "Load error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        App.instance,
                        R.string.post_not_found,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setFormEnabled(state: Boolean) {
        model.editMessage.isEnabled = state
        model.buttonSend.isEnabled = state
    }

    private fun onReply(newrid: Int, txt: String) {
        rid = newrid
        if (rid > 0) {
            val ssb = SpannableStringBuilder()
            val inreplyto = resources.getString(R.string.In_reply_to_) + " "
            ssb.append(inreplyto).append(txt)
            ssb.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                inreplyto.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            model.textReplyTo.text = ssb
            model.textReplyTo.visibility = View.VISIBLE
        } else {
            model.textReplyTo.visibility = View.GONE
        }
    }

    @Throws(FileNotFoundException::class)
    fun postReply(body: String?) {
        model.progressBar.visibility = View.VISIBLE
        App.instance.sendMessage(lifecycleScope, messagePosted, body, attachmentUri, attachmentMime)
    }
}