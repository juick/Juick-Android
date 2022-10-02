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
package com.juick.android.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import com.juick.App
import com.juick.R
import com.juick.android.*
import com.juick.android.JuickMessagesAdapter.OnLoadMoreRequestListener
import com.juick.databinding.FragmentPostsPageBinding
import kotlinx.coroutines.launch

/**
 * Created by gerc on 03.06.2016.
 */
open class FeedFragment: Fragment() {
    internal lateinit var vm: FeedViewModel

    private var _binding: FragmentPostsPageBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = JuickMessagesAdapter()
        binding.list.adapter = adapter
        adapter.setOnItemClickListener { _, pos ->
            adapter.getItem(pos)?.let {
                post ->
                val threadArgs = Bundle()
                threadArgs.putInt("mid", post.mid)
                findNavController(view).navigate(R.id.thread, threadArgs)
            }
        }
        var firstPage = true
        var loading = false
        adapter.setOnLoadMoreRequestListener(
            object : OnLoadMoreRequestListener {
                override fun onLoadMore() {
                    if (loading) return
                    loading = true
                    adapter.getItem(adapter.itemCount - 1)?.let {
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
            firstPage = true
            vm.apiUrl.value = Utils.buildUrl(vm.apiUrl.value)
                .appendQueryParameter("ts", "${System.currentTimeMillis()}")
                .build().toString()
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ProfileData.userProfile.collect {
                    adapter.setOnMenuListener(JuickMessageMenuListener(requireActivity(), it, adapter.items))
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.feed.collect { resource ->
                    when(resource.status) {
                        Status.LOADING -> {
                            if (firstPage) {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.list.visibility = View.GONE
                            }
                        }
                        Status.SUCCESS -> {
                            loading = false
                            stopRefreshing()
                            resource.data?.let {
                                if (firstPage) {
                                    adapter.newData(it)
                                } else {
                                    adapter.addData(it)
                                }
                            }
                        }
                        Status.ERROR -> {
                            loading = false
                            stopRefreshing()
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
    private fun stopRefreshing() {
        binding.swipeContainer.isRefreshing = false
        binding.list.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}