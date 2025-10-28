/*
 * Copyright (C) 2008-2025, Juick
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
package com.juick.android.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.juick.App
import com.juick.R
import com.juick.android.Account
import com.juick.android.JuickMessageMenuListener
import com.juick.android.Utils
import com.juick.android.Utils.replaceUriParameter
import com.juick.android.screens.FeedAdapter.OnLoadMoreRequestListener
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.databinding.FragmentPostsPageBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by gerc on 03.06.2016.
 */
open class FeedFragment: Fragment(R.layout.fragment_posts_page) {
    protected val vm by viewModels<FeedViewModel>()
    internal val account by activityViewModels<Account>()
    private val binding by viewBinding(FragmentPostsPageBinding::bind)
    private var firstPage = true
    private val _postsKey = "posts"
    private val messagePosted = MutableStateFlow<Result<PostResponse>?>(null)
    private val apiResponded = MutableStateFlow<Result<Post>?>(null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        account.profile.observe(viewLifecycleOwner) {
            it?.let { user ->
                val adapter = FeedAdapter(user)
                adapter.setOnItemClickListener { _, pos ->
                    adapter.currentList[pos]?.let { post ->
                        val threadArgs = Bundle()
                        threadArgs.putInt("mid", post.mid)
                        findNavController(this).navigate(R.id.thread, threadArgs)
                    }
                }
                adapter.setOnLoadMoreRequestListener(
                    object : OnLoadMoreRequestListener {
                        override fun onLoadMore() {
                            if (vm.feed.value == null) return
                            adapter.currentList[adapter.itemCount - 1]?.let { lastItem ->
                                val requestUrl = Utils.buildUrl(vm.apiUrl.value)
                                    .build()
                                    .replaceUriParameter("before_mid", lastItem.mid.toString())
                                    .toString()
                                firstPage = false
                                vm.apiUrl.value = requestUrl
                            }
                        }
                    })
                adapter.setOnMenuListener(
                    JuickMessageMenuListener(
                        requireActivity(), this, messagePosted,
                        apiResponded,user
                    )
                )

                lifecycleScope.launch {
                    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        vm.feed.collect { result ->
                            when (result) {
                                null -> {
                                    if (firstPage) {
                                        binding.progressBar.visibility = View.VISIBLE
                                        binding.feedList.visibility = View.GONE
                                        binding.errorText.visibility = View.GONE
                                    }
                                }

                                else -> {
                                    result.fold(
                                        onSuccess = { posts ->
                                            stopRefreshing()
                                            if (posts.isNotEmpty()) {
                                                posts.let {
                                                    val needToScroll =
                                                        haveNewPosts(adapter.currentList, it)
                                                    val newList = if (firstPage) {
                                                        it
                                                    } else {
                                                        adapter.currentList + it
                                                    }
                                                    adapter.submitList(newList)
                                                    vm.state[_postsKey] = newList
                                                    if (needToScroll) {
                                                        binding.feedRefreshButton.isVisible = true
                                                    }
                                                }
                                                vm.feedReceived()
                                            }
                                        },
                                        onFailure = { exception ->
                                            stopRefreshing()
                                            Toast.makeText(
                                                requireContext(),
                                                exception.message,
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                            if (firstPage) {
                                                setError(exception.message ?: getString(R.string.Error))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                binding.feedList.adapter = adapter
                val initialState: List<Post>? = vm.state[_postsKey]
                if (initialState != null) {
                    adapter.submitList(initialState)
                }
            }
        }

        binding.swipeContainer.setColorSchemeColors(
            ContextCompat.getColor(
                App.instance,
                R.color.colorAccent
            )
        )

        binding.swipeContainer.setOnRefreshListener {
            refreshFeed()
        }
        binding.feedRefreshButton.setOnClickListener {
            binding.feedList.postDelayed({
                binding.feedList.layoutManager?.scrollToPosition(0)
                binding.feedRefreshButton.isVisible = false
            }, 200)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                messagePosted.collect { response ->
                    when (response) {
                        null -> {}
                        else -> {
                            response.fold(
                                onSuccess = {
                                    Toast.makeText(activity, it.text, Toast.LENGTH_LONG).show()
                                    account.refresh()
                                    refreshFeed()
                                },
                                onFailure = {

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
                                    account.refresh()
                                    refreshFeed()
                                },
                                onFailure = {

                                }
                            )
                            apiResponded.update { null }
                        }
                    }
                }
            }
        }
    }
    private fun refreshFeed() {
        firstPage = true
        val newUrl = Utils.buildUrl(vm.apiUrl.value)
            .build()
            .replaceUriParameter("before_mid", "")
            .replaceUriParameter("ts", "${System.currentTimeMillis()}")
            .toString()
        vm.apiUrl.value = newUrl
        account.refresh()
    }
    private fun haveNewPosts(oldPosts: List<Post>, newPosts: List<Post>): Boolean {
        if (oldPosts.isEmpty() || newPosts.isEmpty()) {
            return false
        }
        return oldPosts.maxBy { it.mid }.mid < newPosts.maxBy { it.mid }.mid
    }
    private fun stopRefreshing() {
        binding.swipeContainer.isRefreshing = false
        binding.feedList.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.errorText.visibility = View.GONE
    }
    private fun setError(message: String) {
        binding.feedList.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    override fun onResume() {
        super.onResume()
        refreshFeed()
    }
}