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
package com.juick.android

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.juick.App
import com.juick.R
import com.juick.android.fragment.ThreadFragmentArgs
import com.juick.android.screens.FeedAdapter
import com.juick.android.screens.blog.BlogFragmentArgs
import com.juick.api.model.Post
import com.juick.api.model.PostResponse
import com.juick.api.model.User
import kotlinx.coroutines.launch

/**
 *
 * @author Ugnich Anton
 */
class JuickMessageMenuListener(
    private val activity: Context, private val adapter: FeedAdapter, private val me: User) : FeedAdapter.OnItemClickListener {
    private fun confirmAction(context: Context, resId: Int, action: Runnable) : Boolean {
        val builder = AlertDialog.Builder(context)
        var result = false
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(context.resources.getString(resId))
        builder.setPositiveButton(R.string.Yes) { _, _ -> action.run(); result = true }
        builder.setNegativeButton(R.string.Cancel, null)
        builder.show()
        return result
    }

    private fun likeMessage(post: Post) : Boolean {
        return confirmAction(
            activity,
            R.string.Are_you_sure_recommend
        ) {
            processCommand("! #${post.mid}") {
                adapter.postUpdatedListener?.postLikeChanged(post, true)
            }
        }
    }

    private fun subscribeMessageToggle(post: Post): Boolean {
        return if (post.subscribed) {
            confirmAction(
                activity,
                R.string.unsubscribe_from_comments
            ) {
                processCommand("U #${post.mid}") {
                    adapter.postUpdatedListener?.postSubscriptionChanged(post, false)
                }

            }
        } else {
            confirmAction(
                activity,
                R.string.subscribe_to_comments
            ) {
                processCommand("S #${post.mid}") {
                    adapter.postUpdatedListener?.postSubscriptionChanged(post, true)
                }
            }
        }
    }

    override fun onItemClick(view: View?, post: Post) {
        val context = view?.context as Context
        val popupMenu = PopupMenu(context, view)
        if (me.uid == 0) {
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_BLOG, Menu.NONE,
                "@${post.user.uname} ${context.getString(R.string.blog)}"
            )
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_SHARE, Menu.NONE,
                context.getString(R.string.Share)
            )
        } else if (post.user.uid == me.uid) {
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_SHARE, Menu.NONE,
                context.resources.getString(R.string.Share)
            )
            val itemText = if (post.rid == 0)
                context.getString(R.string.DeletePost) else
                context.getString(R.string.DeleteComment)
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_DELETE_POST, Menu.NONE,
                itemText
            )
        } else {
            if (post.rid == 0) {
                popupMenu.menu.add(
                    Menu.NONE, MENU_ACTION_RECOMMEND, Menu.NONE,
                    context.getString(R.string.Recommend_message)
                )
            }
            val userName = post.user.uname
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_BLOG, Menu.NONE,
                "@$userName ${view.context.getString(R.string.blog)}"
            )
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_SUBSCRIBE, Menu.NONE,
                context.resources.getString(R.string.Subscribe_to) + " @" + userName
            )
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_BLACKLIST, Menu.NONE,
                context.resources.getString(R.string.Blacklist) + " @" + userName
            )
            popupMenu.menu.add(
                Menu.NONE, MENU_ACTION_SHARE, Menu.NONE,
                context.resources.getString(R.string.Share)
            )
        }
        popupMenu.setOnMenuItemClickListener {
            val action = it.itemId
            val mid = post.mid
            val rid = post.rid
            val uname = post.user.uname
            when (action) {
                MENU_ACTION_BLOG -> {
                    val navController = Navigation.findNavController(view)
                    val args = BlogFragmentArgs.Builder(uname)
                        .build()
                    navController.navigate(R.id.blog, args.toBundle())
                    true
                }
                MENU_ACTION_RECOMMEND -> likeMessage(post)
                MENU_ACTION_SUBSCRIBE -> confirmAction(
                    activity,
                    R.string.Are_you_sure_subscribe
                ) {
                    processCommand("S @${uname}")
                }
                MENU_ACTION_BLACKLIST -> confirmAction(
                    activity,
                    R.string.Are_you_sure_blacklist
                ) {
                    processCommand("BL @${uname}")
                }
                MENU_ACTION_DELETE_POST -> confirmAction(activity, R.string.Are_you_sure_delete) {
                    processCommand("D #" +
                            if (rid == 0) "$mid" else "$mid/$rid")
                    val navController = Navigation.findNavController(view)
                    navController.popBackStack(R.id.home, false)
                    if (rid > 0) {
                        val args = ThreadFragmentArgs.Builder()
                            .setMid(mid)
                            .setScrollToEnd(true)
                            .build()
                        navController.navigate(R.id.thread, args.toBundle())
                    } else {
                        navController.navigate(R.id.home)
                    }
                }
                MENU_ACTION_SHARE -> {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, "https://juick.com/m/$mid")
                    activity.startActivity(intent)
                    true
                }
                else -> { false }
            }
        }
        popupMenu.show()
    }

    override fun onLikeClick(view: View?, post: Post) {
        likeMessage(post)
    }

    override fun onSubscribeToggleClick(view: View?, post: Post) {
        subscribeMessageToggle(post)
    }

    private fun processCommand(command: String, callback: ((PostResponse) -> Unit)? = null) {
        (activity as LifecycleOwner).lifecycleScope.launch {
            App.instance.sendMessage(command) {
                Toast.makeText(activity, it.text, Toast.LENGTH_LONG).show()
                callback?.invoke(it)
            }
        }
    }

    companion object {
        private const val MENU_ACTION_RECOMMEND = 1
        private const val MENU_ACTION_BLOG = 2
        private const val MENU_ACTION_SUBSCRIBE = 3
        private const val MENU_ACTION_BLACKLIST = 4
        private const val MENU_ACTION_SHARE = 5
        private const val MENU_ACTION_DELETE_POST = 6
    }
}