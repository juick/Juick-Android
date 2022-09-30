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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juick.R
import com.juick.android.Status.*
import com.juick.api.model.Chat
import com.juick.databinding.FragmentDialogListBinding
import com.stfalcon.chatkit.dialogs.DialogsListAdapter

/**
 *
 * @author ugnich
 */
class ChatsFragment : Fragment(R.layout.fragment_dialog_list) {
    private var _model: FragmentDialogListBinding? = null
    private val model get() = _model!!
    private lateinit var vm: ChatsViewModel
    private val chatsAdapter: DialogsListAdapter<Chat> =
        DialogsListAdapter { imageView: ImageView, url: String?, _: Any? ->
            Glide.with(imageView.context)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }

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
        vm.chats.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                SUCCESS -> {
                    model.dialogsList.visibility = View.VISIBLE
                    model.progressBar.visibility = View.GONE
                    resource.data?.let { chats ->
                        chatsAdapter.setItems(chats)
                    }
                }
                ERROR -> TODO()
                LOADING -> {
                    model.dialogsList.visibility = View.GONE
                    model.progressBar.visibility = View.VISIBLE
                }
            }
        }
        vm.isAuthenticated().observe(viewLifecycleOwner) { authenticated ->
            val navController = findNavController(requireView())
            if (!authenticated) {
                val action = ChatsFragmentDirections.actionChatsToNoAuth()
                navController.navigate(action)
            } else {
                navController.popBackStack(R.id.chats, false)
            }
        }
    }

    override fun onDestroyView() {
        _model = null
        super.onDestroyView()
    }
}