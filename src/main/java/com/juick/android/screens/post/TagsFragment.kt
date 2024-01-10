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
package com.juick.android.screens.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.juick.R
import com.juick.databinding.FragmentTagsListBinding
import com.juick.databinding.ItemTagBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding

/**
 *
 * @author Ugnich Anton
 */
class TagsFragment : BottomSheetDialogFragment(R.layout.fragment_tags_list) {
    private val vm by viewModels<TagsViewModel>()
    private val model by viewBinding(FragmentTagsListBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.progressBar.visibility = View.VISIBLE
        model.list.setHasFixedSize(true)
        val adapter = TagsAdapter()
        model.list.adapter = adapter
        adapter.itemClickListener = { _, position ->
            val tag = adapter.currentList[position]
            val navController = NavHostFragment.findNavController(this)
            navController.previousBackStackEntry?.savedStateHandle?.set("tag", tag)
            dismiss()
        }
        /* adapter.setOnItemLongClickListener((view12, position) -> ((BaseActivity) getActivity())
                .replaceFragment(FeedBuilder.feedFor(
                        UrlBuilder.getPostsByTag(uid, adapter.getItem(position)))));*/
        vm.tags.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                null -> {
                    model.list.visibility = View.GONE
                    model.progressBar.visibility = View.VISIBLE
                }

                else -> {
                    model.progressBar.visibility = View.GONE
                    model.list.visibility = View.VISIBLE
                    resource.fold(
                        onSuccess = { tags ->
                            adapter.submitList(tags.map {
                                it.tag
                            })
                        },
                        onFailure = {
                            Toast.makeText(
                                requireContext(),
                                R.string.network_error,
                                Toast.LENGTH_LONG
                            ).show()
                        })
                }
            }
        }
    }

    internal class TagsAdapter : ListAdapter<String, TagsAdapter.TagsViewHolder>(DIFF_CALLBACK) {
        var itemClickListener: ((View?, Int) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsViewHolder {
            return TagsViewHolder(
                ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: TagsViewHolder, position: Int) {
            val tag = getItem(position)
            holder.binding.text.text = tag
            holder.itemView.setOnClickListener { v ->
                itemClickListener?.invoke(v, position)
            }
        }

        internal class TagsViewHolder(val binding: ItemTagBinding) :
            RecyclerView.ViewHolder(binding.root)

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}