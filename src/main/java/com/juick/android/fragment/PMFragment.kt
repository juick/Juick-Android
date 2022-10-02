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
package com.juick.android.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juick.App
import com.juick.R
import com.juick.android.ProfileData
import com.juick.android.widget.util.ViewUtil
import com.juick.api.model.Post
import com.juick.databinding.FragmentPmBinding
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author ugnich
 */
class PMFragment : Fragment(R.layout.fragment_pm) {
    private var _model: FragmentPmBinding? = null
    private val model get() = _model!!
    private lateinit var adapter: MessagesListAdapter<Post>
    private lateinit var uname: String
    private val args by navArgs<PMFragmentArgs>()

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _model = FragmentPmBinding.bind(view)
        uname = args.uname
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                App.instance.api.pm(uname).let { newPms ->
                    withContext(Dispatchers.Main) {
                        adapter.addToEnd(newPms, false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(App.instance, R.string.network_error, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
        model.input.setInputListener { input: CharSequence ->
            postText(input.toString())
            ViewUtil.hideKeyboard(activity)
            true
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ProfileData.userProfile.collect {
                    adapter = MessagesListAdapter(it.uname) { imageView, url, _ ->
                        Glide.with(imageView.context)
                            .load(url)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView)
                    }
                    model.messagesList.setAdapter(adapter)
                }
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                App.instance.newMessage.collect { post ->
                    onNewMessages(listOf(post))
                }
            }
        }
    }

    fun onNewMessages(posts: List<Post>) {
        Log.d("onNewMessages", posts.toString())
        for (p in posts) {
            adapter.addToStart(p, true)
        }
    }

    fun postText(body: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                App.instance.api.postPm(uname, body).let { post ->
                    withContext(Dispatchers.Main) {
                        onNewMessages(listOf(post))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // TODO: blacklist
                    Toast.makeText(App.instance, R.string.network_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        _model = null
        super.onDestroyView()
    }
}