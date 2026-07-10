/*
 * Copyright (C) 2008-2026, Juick
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
package com.juick.android

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope

import com.juick.App
import com.juick.R
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.api.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 * @author Ugnich Anton
 */
class JuickMessageMenuListener(
    private val activity: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val receiver: MutableStateFlow<Result<PostResponse>?>,
    private val postReceiver: MutableStateFlow<Result<Post>?>,
    private val me: User,
    private val onDeletePostNavigate: () -> Unit = {},
) : OnItemClickListener {

    private fun confirmAction(context: Context, resId: Int, action: Runnable): Boolean {
        val builder = AlertDialog.Builder(context)
        var result = false
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(context.resources.getString(resId))
        builder.setPositiveButton(R.string.Yes) { _, _ -> action.run(); result = true }
        builder.setNegativeButton(R.string.Cancel, null)
        builder.show()
        return result
    }

    private suspend fun likeMessage(post: Post): Result<Post>? {
        return runCatching {
            App.instance.api.like(post.mid)
        }
    }

    private fun vipToggle(user: User, completion: ((Boolean) -> Unit)? = null) {
        val scope = lifecycleOwner.lifecycleScope
        val account = (activity as? MainActivity)?.account
        scope.launch {
            val result = withContext(Dispatchers.IO) { App.instance.api.toggleVIP(user.uname) }
            completion?.invoke(result.isSuccessful)
            account?.refresh()
        }
    }
    private fun privacyToggle(post: Post, completion: ((Boolean) -> Unit)? = null) {
        val scope = lifecycleOwner.lifecycleScope
        scope.launch {
            val result = withContext(Dispatchers.IO) { App.instance.api.togglePrivacy(post.mid) }
            completion?.invoke(result.isSuccessful)
        }
    }

    private suspend fun subscribeMessageToggle(post: Post): Result<Post> {
        return runCatching {
            App.instance.api.subscribe(post.mid)
        }
    }

    override fun onItemClick(view: View?, post: Post) {
        val context = view?.context as Context
        val popupMenu = PopupMenu(context, view)
        popupMenu.setForceShowIcon(true)
        popupMenu.menu.add(
            Menu.NONE, MENU_ACTION_SHARE, Menu.NONE,
            context.getString(R.string.Share)
        )
        if (post.user.uid == me.uid) {
            if (me.premium || me.admin) {
                if (post.rid == 0) {
                    val action =
                        if (post.friendsOnly) MENU_ACTION_MAKE_PUBLIC else MENU_ACTION_MAKE_PRIVATE
                    val title =
                        if (post.friendsOnly) context.getString(R.string.make_public) else context.getString(
                            R.string.make_private
                        )
                    val item = popupMenu.menu.add(
                        Menu.NONE, action, Menu.NONE,
                        title
                    )
                    val icon =
                        if (post.friendsOnly) R.drawable.ic_ei_unlock else R.drawable.ic_ei_lock
                    item.icon = ContextCompat.getDrawable(context, icon)
                }
            }
            val itemText = if (post.rid == 0)
                context.getString(R.string.DeletePost) else
                context.getString(R.string.DeleteComment)
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_DELETE_POST, Menu.NONE,
                itemText
            )
        }
        popupMenu.setOnMenuItemClickListener {
            val mid = post.mid
            val rid = post.rid
            when (it.itemId) {
                MENU_ACTION_SHARE -> {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, "https://juick.com/m/$mid")
                    activity.startActivity(intent)
                    true
                }
                MENU_ACTION_DELETE_POST -> confirmAction(activity, R.string.Are_you_sure_delete) {
                    processCommand("D #" +
                            if (rid == 0) "$mid" else "$mid/$rid")
                    onDeletePostNavigate()
                }
                MENU_ACTION_MAKE_PUBLIC -> {
                    confirmAction(activity, R.string.confirm_make_public) {
                        privacyToggle(post)
                    }
                }
                MENU_ACTION_MAKE_PRIVATE -> {
                    confirmAction(activity, R.string.confirm_make_private) {
                        privacyToggle(post)
                    }
                }
                else -> { false }
            }
        }
        popupMenu.show()
    }

    private fun handleLike(post: Post) {
        val scope = lifecycleOwner.lifecycleScope
        scope.launch {
            postReceiver.update {
                likeMessage(post)
            }
        }
    }

    override fun onLikeClick(view: View?, post: Post) {
        if (post.liked) {
            handleLike(post)
        } else {
            confirmAction(activity, R.string.Are_you_sure_recommend) {
                handleLike(post)
            }
        }
    }

    override fun onSubscribeToggleClick(post: Post) {
        val scope = lifecycleOwner.lifecycleScope
        scope.launch {
            postReceiver.update {
                subscribeMessageToggle(post)
            }
        }
    }

    override fun onLinkClick(url: String) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.processUri(url.toUri())
        } else {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            activity.startActivity(intent)
        }
    }

    private fun processCommand(command: String) {
        val scope = lifecycleOwner.lifecycleScope
        App.instance.sendMessage(scope = scope, receiver = receiver, txt = command)
    }

    companion object {
        private const val MENU_ACTION_SUBSCRIBE = 3
        private const val MENU_ACTION_SHARE = 5
        private const val MENU_ACTION_DELETE_POST = 6
        private const val MENU_ACTION_ADD_TO_VIP = 7
        private const val MENU_ACTION_REMOVE_FROM_VIP = 8
        private const val MENU_ACTION_ADD_TO_IGNORELIST = 9
        private const val MENU_ACTION_REMOVE_FROM_IGNORELIST = 10
        private const val MENU_ACTION_MAKE_PRIVATE = 11
        private const val MENU_ACTION_MAKE_PUBLIC = 12
    }
}
