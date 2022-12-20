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
package com.juick.android.screens.pm

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juick.App
import com.juick.R
import com.juick.android.ProfileData
import com.juick.android.Status
import com.juick.android.widget.util.hideKeyboard
import com.juick.api.model.Post
import com.juick.databinding.FragmentPmBinding
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author ugnich
 */
class PMFragment : Fragment(R.layout.fragment_pm) {
    private val model by viewBinding(FragmentPmBinding::bind)
    private lateinit var vm: PMViewModel
    private lateinit var adapter: MessagesListAdapter<Post>
    private lateinit var uname: String
    private val args by navArgs<PMFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uname = args.uname
        vm = ViewModelProvider(this, PMViewModelFactory(uname))[PMViewModel::class.java]
        model.input.setInputListener { input: CharSequence ->
            postText(input.toString())
            hideKeyboard(activity)
            true
        }
        ProfileData.userProfile.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).onEach {
            adapter = MessagesListAdapter(it.uname) { imageView, url, _ ->
                Glide.with(imageView.context)
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            }
            model.messagesList.setAdapter(adapter)
        }.launchIn(lifecycleScope)
        App.instance.messages.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).onEach { posts ->
            onNewMessages(posts.filter {
                it.isOurs(uname)
            })
        }.launchIn(lifecycleScope)
        vm.messages.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).onEach {
            when (it.status) {
                Status.LOADING -> {}
                Status.ERROR -> Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                Status.SUCCESS -> {
                    it.data?.let { posts ->
                        adapter.addToEnd(posts, false)
                    }
                }
            }
        }.launchIn(lifecycleScope)
        vm.loadMessages()
    }

    private fun Post.isOurs(authorName: String) : Boolean {
        return mid == 0 && uname == authorName || uname == to?.uname
    }

    private fun onNewMessages(posts: List<Post>) {
        Log.d("onNewMessages", posts.toString())
        posts.forEach {
            adapter.addToStart(it, shouldScrollToBottom)
        }
    }

    private fun postText(body: String) {
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

    private val shouldScrollToBottom : Boolean
        get() {
            val lastVisible =
                (model.messagesList.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            val total = adapter.itemCount - 1
            return lastVisible == total
        }
}