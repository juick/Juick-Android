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

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.SignInActivity.SignInStatus
import com.juick.android.fragment.ThreadFragmentArgs
import com.juick.android.screens.chats.ChatsFragmentDirections
import com.juick.android.screens.home.HomeFragmentDirections
import com.juick.android.screens.search.SearchFragmentArgs
import com.juick.android.updater.Updater
import com.juick.android.widget.util.setAppBarElevation
import com.juick.api.model.Post
import com.juick.databinding.ActivityMainBinding
import com.juick.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * @author Ugnich Anton
 */
class MainActivity : AppCompatActivity() {
    private lateinit var model: ActivityMainBinding
    private var notificationManager: NotificationManager? = null
    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private fun showLogin() {
        if (!Utils.hasAuth()) {
            loginLauncher.launch(Intent(this, SignInActivity::class.java))
        }
    }

    private var avatar: Bitmap? = null
    private lateinit var badge: BadgeDrawable
    private lateinit var appBarConfiguration: AppBarConfiguration

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var requestNotificationsPermission = RequestPermission(
        this, Manifest.permission.POST_NOTIFICATIONS, Build.VERSION_CODES.TIRAMISU)

    @ExperimentalBadgeUtils
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ActivityMainBinding.inflate(layoutInflater)
        setContentView(model.root)
        val toolbar = model.toolbar
        //toolbar.inflateMenu(R.menu.toolbar);
        setSupportActionBar(toolbar)

        setAppBarElevation(model.appbarLayout)
        //CollapsingToolbarLayout layout = model.collapsingToolbarLayout;
        val navView = model.bottomNav
        val fab = model.fab
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration.Builder(
            R.id.home,
            R.id.discover,
            R.id.chats,
            R.id.no_auth,
            R.id.new_post
        )
            .build()
        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        //NavigationUI.setupWithNavController(toolbar, navController);
        setupWithNavController(navView, navController)
        navController.addOnDestinationChangedListener {
                _, destination, _ ->
            val id = destination.id
            var scrollFlags = (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
            if (shouldHideNavView(id)) {
                navView.visibility = View.GONE
                scrollFlags = 0
            } else {
                navView.visibility = View.VISIBLE
            }
            (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = scrollFlags
            val fabVisibility = if (shouldViewFab(id)) View.VISIBLE else View.GONE
            fab.visibility = fabVisibility
        }

        App.instance.authorizationCallback = {
            val updatePasswordIntent = Intent(this, SignInActivity::class.java)
            updatePasswordIntent.putExtra(
                SignInActivity.EXTRA_ACTION,
                SignInActivity.ACTION_PASSWORD_UPDATE
            )
            loginLauncher.launch(updatePasswordIntent)
        }
        loginLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { response: ActivityResult ->
                if (response.resultCode == RESULT_OK) {
                    val intent = intent
                    finish()
                    startActivity(intent)
                }
            }
        model.fab.setOnClickListener {
            if (Utils.hasAuth()) {
                navController.navigate(R.id.new_post)
            } else {
                showLogin()
            }
        }
        lifecycleScope.launch {
            if (Utils.hasAuth()) {
                if (requestNotificationsPermission()) {
                    notificationManager = NotificationManager()
                }
                refresh()
            }
            Updater(this@MainActivity)
                .checkUpdate()
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                ProfileData.refresh()
                App.instance.signInStatus.collect { signInStatus: SignInStatus ->
                    if (signInStatus == SignInStatus.SIGN_IN_PROGRESS) {
                        showLogin()
                    }
                }
            }
        }
        badge = BadgeDrawable.create(this)
        badge.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
        badge.badgeTextColor = ContextCompat.getColor(this, R.color.colorMainBackground)
        model.toolbar.viewTreeObserver.addOnGlobalLayoutListener {
            BadgeUtils.attachBadgeDrawable(
                badge,
                model.toolbar,
                R.id.discussions
            )
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                ProfileData.userProfile.collect {
                    when (it.status) {
                        Status.SUCCESS -> {
                            val user = it.data!!
                            val avatarUrl: String = user.avatar
                            Glide.with(this@MainActivity)
                                .asBitmap()
                                .load(avatarUrl)
                                .placeholder(R.drawable.av_96)
                                .into(object : CustomTarget<Bitmap>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap>?
                                    ) {
                                        avatar = resource
                                        invalidateOptionsMenu()
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        avatar = null
                                    }
                                })
                            withContext(Dispatchers.Main) {
                                if (user.unreadCount > 0) {
                                    badge.isVisible = true
                                    badge.number = user.unreadCount
                                } else {
                                    badge.isVisible = false
                                }
                            }
                        }

                        else -> { avatar = ResourcesCompat.getDrawable(resources, R.drawable.av_96, null)!!.toBitmap() }
                    }

                }
            }
        }
    }

    private fun shouldHideNavView(view: Int): Boolean {
        when (view) {
            R.id.thread, R.id.PMFragment, R.id.new_post, R.id.tags -> return true
        }
        return false
    }

    private fun shouldViewFab(view: Int): Boolean {
        return view == R.id.home || view == R.id.discover
    }

    override fun onResume() {
        super.onResume()
        notificationManager?.onResume()
        val intent = intent
        val action = StringUtils.defaultString(intent.action)
        if (action == BuildConfig.INTENT_NEW_EVENT_ACTION) {
            intent.action = ""
            val msg = intent.getStringExtra(getString(R.string.notification_extra))
            try {
                val jmsg = App.instance.jsonMapper.readValue(msg, Post::class.java)
                if (jmsg.user.uid == 0) {
                    setTitle(R.string.Discussions)
                    //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
                } else {
                    if (jmsg.mid == 0) {
                        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
                        val navController = navHostFragment.navController
                        navController.popBackStack(R.id.chats, true)
                        navController.navigate(R.id.chats)
                        val chatAction =
                            ChatsFragmentDirections.actionChatsToPMFragment(jmsg.user.uname)
                        chatAction.uid = jmsg.user.uid
                        navController.navigate(chatAction)
                    } else {
                        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
                        val navController = navHostFragment.navController
                        navController.popBackStack(R.id.home, false)
                        val discoverAction =
                            HomeFragmentDirections.actionDiscoverFragmentToThreadFragment()
                        discoverAction.mid = jmsg.mid
                        discoverAction.scrollToEnd = true
                        navController.navigate(discoverAction)
                    }
                }
            } catch (e: IOException) {
                Log.d(this.javaClass.simpleName, "Invalid JSON data", e)
            }
        }
        if (action == Intent.ACTION_VIEW) {
            val data = intent.data
            data?.let { processUri(it) }
            intent.action = ""
        }
        if (action == Intent.ACTION_SEND) {
            val mime = intent.type
            val extras = intent.extras as Bundle
            val postArgs = Bundle()
            if (mime == "text/plain") {
                postArgs.putString("text", extras.getString(Intent.EXTRA_TEXT))
            } else {
                postArgs.putString("uri", extras.getString(Intent.EXTRA_STREAM))
            }
            val navHostFragment = model.navHost.getFragment<NavHostFragment>()
            val navController = navHostFragment.navController
            navController.navigate(R.id.new_post, postArgs)
            intent.action = ""
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                model.toolbar.collapseActionView()
                val navHostFragment = model.navHost.getFragment<Fragment>() as NavHostFragment
                val navController = navHostFragment.navController
                val args = SearchFragmentArgs.Builder(s).build()
                navController.navigate(R.id.search, args.toBundle())
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return true
            }
        })
        val profileItem = menu.findItem(R.id.profile)
        if (profileItem != null && avatar != null) {
            profileItem.actionView?.findViewById<ImageView>(R.id.profile_image)
                ?.setImageBitmap(avatar)
            profileItem.actionView?.setOnClickListener {
                onOptionsItemSelected(profileItem)
            }
        }
        val discussionsItem = menu.findItem(R.id.discussions)
        if (discussionsItem != null) {
            discussionsItem.actionView?.setOnClickListener {
                onOptionsItemSelected(discussionsItem)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    fun processUri(data: Uri) {
        val pathSegments = data.pathSegments
        val navHostFragment = model.navHost.getFragment<Fragment>() as NavHostFragment
        val navController = navHostFragment.navController
        when (pathSegments.size) {
            1 -> {}
            2 -> {
                // thread
                val threadId = pathSegments[1]
                val args = ThreadFragmentArgs.Builder()
                    .setMid(threadId.toInt())
                    .build()
                navController.popBackStack(R.id.home, false)
                navController.navigate(R.id.thread, args.toBundle())
            }
            else ->                 // discover
                navController.navigate(R.id.home)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        return (onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item))
    }
}