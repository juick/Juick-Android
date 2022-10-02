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
package com.juick.android.screens.chats

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juick.android.ProfileData
import com.juick.android.Status.*
import com.juick.api.model.Chat
import com.juick.databinding.FragmentDialogListBinding
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.coroutines.launch

/**
 *
 * @author ugnich
 */
class ChatsFragment : Fragment() {
    private var _model: FragmentDialogListBinding? = null
    private val model get() = _model!!
    private val chatsAdapter: DialogsListAdapter<Chat> =
        DialogsListAdapter { imageView: ImageView, url: String?, _: Any? ->
            Glide.with(imageView.context)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }

    private lateinit var vm: ChatsViewModel

    init {
        chatsAdapter.setOnDialogClickListener { dialog: Chat ->
            val navController = findNavController(requireView())
            val action = ChatsFragmentDirections.actionChatsToPMFragment(dialog.dialogName)
            action.uid = dialog.id.toInt()
            action.uname = dialog.dialogName
            navController.navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _model = FragmentDialogListBinding.inflate(inflater, container, false)
        return model.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.dialogsList.setAdapter(chatsAdapter)
        vm = ViewModelProvider(this)[ChatsViewModel::class.java]
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ProfileData.userProfile.collect { me ->
                    val navController = findNavController(requireView())
                    if (me.uid == 0) {
                        val action = ChatsFragmentDirections.actionChatsToNoAuth()
                        navController.navigate(action)
                    } else {
                        vm.loadChats()
                        vm.chats.collect { resource ->
                            when (resource.status) {
                                SUCCESS -> {
                                    model.dialogsList.visibility = View.VISIBLE
                                    model.progressBar.visibility = View.GONE
                                    resource.data?.let { chats ->
                                        chatsAdapter.setItems(chats)
                                    }
                                }
                                ERROR -> Toast.makeText(
                                    requireContext(),
                                    resource.message,
                                    Toast.LENGTH_LONG
                                ).show()
                                LOADING -> {
                                    model.dialogsList.visibility = View.GONE
                                    model.progressBar.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _model = null
        super.onDestroyView()
    }
}