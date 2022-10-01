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

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juick.App
import com.juick.R
import com.juick.android.widget.util.ViewUtil
import com.juick.api.model.Post
import com.juick.databinding.FragmentPmBinding
import com.stfalcon.chatkit.messages.MessagesListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author ugnich
 */
class PMFragment : Fragment(R.layout.fragment_pm) {
    private var model: FragmentPmBinding? = null
    private var adapter: MessagesListAdapter<Post>? = null
    private var uname: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model = FragmentPmBinding.bind(view)
        val arguments = arguments
        if (arguments != null) {
            uname = PMFragmentArgs.fromBundle(requireArguments()).uname
            adapter = MessagesListAdapter(java.lang.String.valueOf(App.instance.me.value)
            ) { imageView, url, _ ->
                Glide.with(imageView.context)
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            }
            model!!.messagesList.setAdapter(adapter)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    App.instance.api.pm(uname)?.let {
                        newPms ->
                        withContext(Dispatchers.Main) {
                            adapter?.addToEnd(newPms, false)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(App.instance, R.string.network_error, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
            model!!.input.setInputListener { input: CharSequence ->
                postText(input.toString())
                ViewUtil.hideKeyboard(activity)
                true
            }
            App.instance.newMessage.observe(viewLifecycleOwner) { post -> onNewMessages(listOf(post)) }
        }
    }

    fun onNewMessages(posts: List<Post>) {
        Log.d("onNewMessages", posts.toString())
        if (adapter != null) {
            for (p in posts) {
                adapter!!.addToStart(p, true)
            }
        }
    }

    fun postText(body: String?) {
        CoroutineScope(Dispatchers.IO).launch {
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
        model = null
        super.onDestroyView()
    }
}