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
package com.juick.android.screens.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.juick.R
import com.juick.databinding.FragmentTagsListBinding
import com.juick.databinding.ItemTagBinding
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.coroutines.launch

/**
 *
 * @author Ugnich Anton
 */
class TagsFragment : BottomSheetDialogFragment(R.layout.fragment_tags_list) {
    private val model by viewBinding(FragmentTagsListBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.progressBar.visibility = View.VISIBLE
        model.list.setHasFixedSize(true)
        val adapter = TagsAdapter()
        model.list.adapter = adapter
        adapter.setOnItemClickListener { _, position ->
            val tag = adapter.currentList[position]
            val navController = NavHostFragment.findNavController(this)
            navController.previousBackStackEntry?.savedStateHandle?.set("tag", tag)
            dismiss()
        }
        /* adapter.setOnItemLongClickListener((view12, position) -> ((BaseActivity) getActivity())
                .replaceFragment(FeedBuilder.feedFor(
                        UrlBuilder.getPostsByTag(uid, adapter.getItem(position)))));*/
        val vm = ViewModelProvider(this)[TagsViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.tags.collect { resource ->
                    when (resource) {
                        null -> {
                            model.list.visibility = View.GONE
                            model.progressBar.visibility = View.VISIBLE
                        }
                        else -> resource.fold(
                        onSuccess = { tags ->
                            adapter.submitList(tags.map {
                                it.tag
                            })
                        },
                        onFailure = {
                            model.progressBar.visibility = View.GONE
                            model.list.visibility = View.VISIBLE
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
    }

    internal class TagsAdapter : ListAdapter<String, TagsAdapter.TagsViewHolder>(DIFF_CALLBACK) {
        var itemClickListener: ((View?, Int) -> Unit)? = null
        var itemLongClickListener: OnItemLongClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsViewHolder {
            val viewHolder = TagsViewHolder(
                ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            viewHolder.setOnItemClickListener(itemClickListener)
            viewHolder.setOnItemLongClickListener(itemLongClickListener)
            return viewHolder
        }

        override fun onBindViewHolder(holder: TagsViewHolder, position: Int) {
            val tag = getItem(position)
            holder.binding.text.text = tag
        }

        fun setOnItemClickListener(itemClickListener: (View?, Int) -> Unit) {
            this.itemClickListener = itemClickListener
        }

        fun setOnItemLongClickListener(itemClickListener: OnItemLongClickListener?) {
            itemLongClickListener = itemClickListener
        }

        interface OnItemClickListener {
            fun onItemClick(view: View?, pos: Int)
        }

        interface OnItemLongClickListener {
            fun onItemLongClick(view: View?, pos: Int)
        }

        internal class TagsViewHolder(val binding: ItemTagBinding) :
            RecyclerView.ViewHolder(binding.root),
            View.OnClickListener,
            OnLongClickListener {
            var itemClickListener: ((View?, Int) -> Unit)? = null
            var itemLongClickListener: OnItemLongClickListener? = null

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            }

            fun setOnItemClickListener(listener: ((View?, Int) -> Unit)?) {
                itemClickListener = listener
            }

            fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
                itemLongClickListener = listener
            }

            override fun onClick(v: View) {
                itemClickListener?.invoke(v, bindingAdapterPosition)
            }

            override fun onLongClick(v: View): Boolean {
                if (itemLongClickListener != null) {
                    itemLongClickListener?.onItemLongClick(v, bindingAdapterPosition)
                    return true
                }
                return false
            }
        }
        companion object {
            val DIFF_CALLBACK = object: DiffUtil.ItemCallback<String>() {
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