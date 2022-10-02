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
package com.juick.android

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.juick.App
import com.juick.R
import com.juick.android.fragment.ThreadFragmentArgs
import com.juick.android.screens.FeedAdapter
import com.juick.api.model.Post
import com.juick.api.model.SecureUser
import kotlinx.coroutines.launch

/**
 *
 * @author Ugnich Anton
 */
class JuickMessageMenuListener(private val activity: Context, private val view: View, private val me: SecureUser, private val postList: List<Post>) : DialogInterface.OnClickListener,
    FeedAdapter.OnItemClickListener {
    private var selectedPost: Post? = null
    private val currentActions = IntArray(MENU_ACTION_SOME_LAST_CMD)
    private fun confirmAction(context: Context, resId: Int, action: Runnable) {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(context.resources.getString(resId))
        builder.setPositiveButton(R.string.Yes) { _, _ -> action.run() }
        builder.setNegativeButton(R.string.Cancel, null)
        builder.show()
    }

    override fun onItemClick(view: View?, pos: Int) {
        val context = view?.context as Context
        selectedPost = postList[pos]
        val items: Array<CharSequence?>
        var menuLength: Int
        if (me.uid == 0) {
            menuLength = 2
            items = listOf(
                "@${selectedPost!!.user.uname} ${context.getString(R.string.blog)}",
                context.getString(R.string.Share)
            ).toTypedArray()
            currentActions[0] = MENU_ACTION_BLOG
            currentActions[1] = MENU_ACTION_SHARE
        } else if (selectedPost!!.user.uid == me.uid) {
            menuLength = 1
            items = arrayOfNulls(menuLength)
            items[0] =
                if (selectedPost!!.rid == 0)
                    context.getString(R.string.DeletePost) else
                    context.getString(R.string.DeleteComment)
            currentActions[0] = MENU_ACTION_DELETE_POST
        } else {
            menuLength = 4
            if (selectedPost!!.rid == 0) {
                menuLength++
            }
            items = arrayOfNulls(menuLength)
            var i = 0
            if (selectedPost!!.rid == 0) {
                items[i++] = context.getString(R.string.Recommend_message)
                currentActions[i - 1] = MENU_ACTION_RECOMMEND
            }
            val UName = selectedPost!!.user.uname
            items[i++] = "@$UName ${view.context.getString(R.string.blog)}"
            currentActions[i - 1] = MENU_ACTION_BLOG
            items[i++] = context.resources.getString(R.string.Subscribe_to) + " @" + UName
            currentActions[i - 1] = MENU_ACTION_SUBSCRIBE
            items[i++] = context.resources.getString(R.string.Blacklist) + " @" + UName
            currentActions[i - 1] = MENU_ACTION_BLACKLIST
            items[i++] = context.resources.getString(R.string.Share)
            currentActions[i - 1] = MENU_ACTION_SHARE
        }
        val builder = AlertDialog.Builder(context)
        builder.setItems(items, this)
        builder.show()
    }

    private fun processCommand(command: String) {
        (activity as LifecycleOwner).lifecycleScope.launch {
            App.instance.sendMessage(command) {
                Toast.makeText(activity, it.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val action = currentActions[which]
        val mid = selectedPost!!.mid
        val rid = selectedPost!!.rid
        val uname = selectedPost!!.user.uname
        when (action) {
            MENU_ACTION_RECOMMEND -> confirmAction(
                activity,
                R.string.Are_you_sure_recommend
            ) {
                processCommand("! #${mid}")
            }
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
                intent.putExtra(Intent.EXTRA_TEXT, "https://juick.com/$mid")
                activity.startActivity(intent)
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
        private const val MENU_ACTION_SOME_LAST_CMD = 7
    }
}