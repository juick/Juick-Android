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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.juick.App
import com.juick.R
import com.juick.android.JuickMessageMenuListener
import com.juick.android.ProfileData
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
    internal lateinit var vm: FeedViewModel

    private val binding by viewBinding(FragmentPostsPageBinding::bind)

    private var firstPage = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = FeedAdapter()
        adapter.postUpdatedListener = this
        adapter.registerAdapterDataObserver(object: AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val linearLayoutManager = binding.list.layoutManager as LinearLayoutManager
                val visiblePosition = linearLayoutManager.findFirstVisibleItemPosition()
                if (positionStart <= visiblePosition) {
                    binding.list.scrollToPosition(0)
                }
            }
        })
        binding.list.adapter = adapter
        adapter.setOnItemClickListener { _, pos ->
            adapter.currentList[pos]?.let {
                post ->
                val threadArgs = Bundle()
                threadArgs.putInt("mid", post.mid)
                findNavController(view).navigate(R.id.thread, threadArgs)
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
                            .appendQueryParameter("before_mid", lastItem.mid.toString())
                            .build().toString()
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ProfileData.userProfile.collect {
                    it?.let { user ->
                        adapter.setOnMenuListener(
                            JuickMessageMenuListener(
                                requireActivity(), adapter, user
                            )
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.feed.collect { result ->
                    when (result) {
                        null -> {
                            if (firstPage) {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.list.visibility = View.GONE
                                binding.errorText.visibility = View.GONE
                            }
                        }

                        else -> result.fold(
                            onSuccess = { posts ->
                                loading = false
                                stopRefreshing()
                                posts.let {
                                    if (firstPage) {
                                        adapter.submitList(it)
                                    } else {
                                        adapter.submitList(adapter.currentList + it)
                                    }
                                }
                            },
                            onFailure = { exception ->
                                loading = false
                                stopRefreshing()
                                Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG)
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
    fun refreshFeed() {
        firstPage = true
        val newUrl = Utils.buildUrl(vm.apiUrl.value)
            .build()
            .replaceUriParameter("ts", "${System.currentTimeMillis()}")
            .toString()
        vm.apiUrl.value = newUrl
        viewLifecycleOwner.lifecycleScope.launch {
            ProfileData.refresh()
        }
    }
    private fun stopRefreshing() {
        binding.swipeContainer.isRefreshing = false
        binding.list.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.errorText.visibility = View.GONE
    }
    private fun setError(message: String) {
        binding.list.visibility = View.GONE
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