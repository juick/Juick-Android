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
package com.juick.android.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.juick.App
import com.juick.R
import com.juick.android.JuickMessageMenuListener
import com.juick.android.Account
import com.juick.android.Utils
import com.juick.android.Utils.replaceUriParameter
import com.juick.android.screens.FeedAdapter.OnLoadMoreRequestListener
import com.juick.api.model.Post
import com.juick.databinding.FragmentPostsPageBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.launch

/**
 * Created by gerc on 03.06.2016.
 */
open class FeedFragment: Fragment(R.layout.fragment_posts_page), FeedAdapter.OnPostUpdatedListener {
    protected val vm by viewModels<FeedViewModel>()
    internal val account by activityViewModels<Account>()
    private val binding by viewBinding(FragmentPostsPageBinding::bind)

    private var firstPage = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = FeedAdapter()
        adapter.postUpdatedListener = this
        binding.feedList.adapter = adapter
        adapter.setOnItemClickListener { _, pos ->
            adapter.currentList[pos]?.let {
                post ->
                val threadArgs = Bundle()
                threadArgs.putInt("mid", post.mid)
                findNavController(this).navigate(R.id.thread, threadArgs)
            }
        }
        var loading = false
        adapter.setOnLoadMoreRequestListener(
            object : OnLoadMoreRequestListener {
                override fun onLoadMore() {
                    if (loading) return
                    loading = true
                    adapter.currentList[adapter.itemCount - 1]?.let {
                        lastItem ->
                        val requestUrl = Utils.buildUrl(vm.apiUrl.value)
                            .build()
                            .replaceUriParameter("before_mid", lastItem.mid.toString())
                            .toString()
                        firstPage = false
                        vm.apiUrl.value = requestUrl
                    }
                }
            })

        binding.swipeContainer.setColorSchemeColors(
            ContextCompat.getColor(
                App.instance,
                R.color.colorAccent
            )
        )

        binding.swipeContainer.setOnRefreshListener {
            refreshFeed()
        }
        account.profile.observe(viewLifecycleOwner) {
            it?.let { user ->
                adapter.setOnMenuListener(
                    JuickMessageMenuListener(
                        requireActivity(), this, adapter, user
                    )
                )
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                                    loading = false
                                    stopRefreshing()
                                    if (posts.size > 0) {
                                        posts.let {
                                            val needToScroll = haveNewPosts(adapter.currentList, it)
                                            if (firstPage) {
                                                adapter.submitList(it)
                                            } else {
                                                adapter.submitList(adapter.currentList + it)
                                            }
                                            if (needToScroll) {
                                                binding.feedList.scrollToPosition(0)
                                            }
                                        }
                                    }
                                },
                                onFailure = { exception ->
                                    loading = false
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
                            vm.feedReceived()
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

    override fun postUpdated(post: Post) {
        refreshFeed()
    }

    override fun postLikeChanged(post: Post, isLiked: Boolean) {

    }

    override fun postSubscriptionChanged(post: Post, isSubscribed: Boolean) {

    }
}