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
package com.juick.android.screens.post

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.juick.R
import com.juick.android.Status
import com.juick.databinding.FragmentTagsListBinding
import kotlinx.coroutines.launch

/**
 *
 * @author Ugnich Anton
 */
class TagsFragment : BottomSheetDialogFragment() {
    private var _model: FragmentTagsListBinding? = null
    private val model get() = _model!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _model = FragmentTagsListBinding.inflate(inflater, container, false)
        return model.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.progressBar.visibility = View.VISIBLE
        model.list.setHasFixedSize(true)
        val adapter = TagsAdapter()
        model.list.adapter = adapter
        adapter.setOnItemClickListener { _, position ->
            val tag = adapter.getItem(position)
            val navController = NavHostFragment.findNavController(this)
            navController.previousBackStackEntry!!.savedStateHandle.set("tag", tag)
            dismiss()
        }
        /* adapter.setOnItemLongClickListener((view12, position) -> ((BaseActivity) getActivity())
                .replaceFragment(FeedBuilder.feedFor(
                        UrlBuilder.getPostsByTag(uid, adapter.getItem(position)))));*/
        val vm = ViewModelProvider(this)[TagsViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.tags.collect { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            model.progressBar.visibility = View.GONE
                            model.list.visibility = View.VISIBLE
                            val tags = resource.data?.map {
                                it.tag
                            } ?: listOf()
                            adapter.addData(tags)
                        }
                        Status.ERROR -> {
                            model.progressBar.visibility = View.GONE
                            model.list.visibility = View.VISIBLE
                            Toast.makeText(
                                requireContext(),
                                R.string.network_error,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Status.LOADING -> {
                            model.list.visibility = View.GONE
                            model.progressBar.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    internal class TagsAdapter : RecyclerView.Adapter<TagsAdapter.VH>() {
        var items: MutableList<String> = ArrayList()
        var itemClickListener: ((View?, Int) -> Unit)? = null
        var itemLongClickListener: OnItemLongClickListener? = null
        fun addData(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val vh = VH(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
            vh.setOnItemClickListener(itemClickListener)
            vh.setOnItemLongClickListener(itemLongClickListener)
            Log.d("TagsAdapter", "onCreateViewHolder")
            return vh
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val tag = items[position]
            Log.d("TagsAdapter", "onBindViewHolder: $position $tag")
            holder.textView.text = tag
        }

        override fun getItemCount(): Int {
            Log.d("TagsAdapter", "getItemCount: " + items.size)
            return items.size
        }

        fun getItem(position: Int): String {
            Log.d("TagsAdapter", "getItem: $position")
            return items[position]
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

        internal class VH(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
            OnLongClickListener {
            var textView: TextView
            var itemClickListener: ((View?, Int) -> Unit)? = null
            var itemLongClickListener: OnItemLongClickListener? = null

            init {
                textView = itemView.findViewById(android.R.id.text1)
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
                if (itemClickListener != null) {
                    itemClickListener!!.invoke(v, bindingAdapterPosition)
                }
            }

            override fun onLongClick(v: View): Boolean {
                if (itemLongClickListener != null) {
                    itemLongClickListener!!.onItemLongClick(v, bindingAdapterPosition)
                    return true
                }
                return false
            }
        }
    }

    override fun onDestroyView() {
        _model = null
        super.onDestroyView()
    }
}