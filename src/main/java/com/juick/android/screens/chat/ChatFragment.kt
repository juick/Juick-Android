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
package com.juick.android.screens.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.juick.App
import com.juick.R
import com.juick.android.Account
import com.juick.android.widget.util.hideKeyboard
import com.juick.android.widget.util.load
import com.juick.api.model.Post
import com.juick.databinding.FragmentChatBinding
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author ugnich
 */
class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val account by activityViewModels<Account>()
    private val model by viewBinding(FragmentChatBinding::bind)
    private lateinit var vm: ChatViewModel
    private lateinit var adapter: MessagesListAdapter<Post>
    private lateinit var uname: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uname = arguments?.getString("uname") ?: ""
        vm = ViewModelProvider(this, ChatViewModelFactory(uname))[ChatViewModel::class.java]
        model.input.setInputListener { input: CharSequence ->
            postText(input.toString())
            hideKeyboard(activity)
            true
        }
        account.profile.observe(viewLifecycleOwner) {
            it?.let { user ->
                adapter = MessagesListAdapter(user.uname) { imageView, url, _ ->
                    imageView.load(url ?: "")
                }
                model.messagesList.setAdapter(adapter)
            }
        }
        App.instance.messages.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).onEach { posts ->
            onNewMessages(posts.filter {
                it.isOurs(uname)
            })
            App.instance.messages.update { listOf() }
        }.launchIn(lifecycleScope)
        vm.messages.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).onEach {
            when (it) {
                null -> {}
                else -> {
                    it.fold(
                        onSuccess = { posts ->
                            adapter.addToEnd(posts, false)
                        },
                        onFailure = { error ->
                            Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG)
                                .show()
                        }
                    )
                    vm.messages.update { Result.success(listOf()) }
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