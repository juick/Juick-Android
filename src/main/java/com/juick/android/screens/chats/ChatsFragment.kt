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
package com.juick.android.screens.chats

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.juick.App
import com.juick.R
import com.juick.android.widget.util.load
import com.juick.api.model.Chat
import com.juick.databinding.FragmentDialogListBinding
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import isAuthenticated
import kotlinx.coroutines.launch

/**
 *
 * @author ugnich
 */
class ChatsFragment : Fragment(R.layout.fragment_dialog_list) {
    private val model by viewBinding(FragmentDialogListBinding::bind)
    private val chatsAdapter: DialogsListAdapter<Chat> =
        DialogsListAdapter { imageView: ImageView, url: String?, _: Any? ->
            imageView.load(url ?: "")
        }

    private lateinit var vm: ChatsViewModel

    init {
        chatsAdapter.setOnDialogClickListener { dialog: Chat ->
            val navController = findNavController(this)
            val action = Bundle()
            action.putString("uname", dialog.dialogName)
            action.putInt("uid", dialog.id.toInt())
            navController.navigate(R.id.PMFragment, action)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.dialogsList.setAdapter(chatsAdapter)
        model.swipeContainer.setColorSchemeColors(
            ContextCompat.getColor(
                App.instance,
                R.color.colorAccent
            )
        )
        model.swipeContainer.setOnRefreshListener {
            lifecycleScope.launch {
                vm.loadChats()
            }
        }
        vm = ViewModelProvider(this)[ChatsViewModel::class.java]

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (App.instance.isAuthenticated) {
                    vm.loadChats()
                    vm.chats.collect { resource ->
                        when (resource) {
                            null -> {
                                startLoading()
                            }

                            else -> resource.fold(
                                onSuccess = { chats ->
                                    stopLoading()
                                    if (chats.isNotEmpty()) {
                                        chatsAdapter.setItems(chats)
                                    } else {
                                        setError(getString(R.string.you_have_no_direct_messages))
                                    }
                                },
                                onFailure = {
                                    stopLoading()
                                    Toast.makeText(
                                        requireContext(),
                                        it.message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                    setError(it.message ?: getString(R.string.Error))
                                }
                            )
                        }
                    }
                } else {
                    val navController = findNavController(this@ChatsFragment)
                    navController.navigate(R.id.no_auth)
                }
            }
        }
    }

    private fun stopLoading() {
        model.dialogsList.visibility = View.VISIBLE
        model.progressBar.visibility = View.GONE
        model.errorText.visibility = View.GONE
        model.swipeContainer.isRefreshing = false
    }

    private fun startLoading() {
        model.dialogsList.visibility = View.GONE
        model.progressBar.visibility = View.VISIBLE
        model.errorText.visibility = View.GONE
    }

    private fun setError(message: String) {
        model.errorText.visibility = View.VISIBLE
        model.errorText.text = message
    }
}