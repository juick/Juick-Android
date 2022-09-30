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
package com.juick.android.screens.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.juick.android.JuickMessagesAdapter
import com.juick.databinding.FragmentPostsPageBinding

class DiscoverFragment : Fragment() {
    private var _binding: FragmentPostsPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: DiscoverViewModel
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
            val post = adapter.getItem(pos)
            val action = DiscoverFragmentDirections.actionDiscoverToThread()
            action.mid = post.mid
            findNavController(view).navigate(action)
        }
        vm = ViewModelProvider(this)[DiscoverViewModel::class.java]
        vm.feed.observe(viewLifecycleOwner) { posts ->
            stopRefreshing()
            adapter.newData(posts)
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