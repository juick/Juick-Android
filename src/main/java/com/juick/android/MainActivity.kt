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
package com.juick.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.SignInActivity.SignInStatus
import com.juick.android.updater.Updater
import com.juick.api.model.Post
import com.juick.databinding.ActivityMainBinding
import com.juick.util.StringUtils
import isAuthenticated
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * @author Ugnich Anton
 */
class MainActivity : AppCompatActivity() {
    val account by viewModels<Account>()
    private lateinit var model: ActivityMainBinding
    private var notificationManager: NotificationManager? = null
    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private fun showLogin() {
        if (!App.instance.isAuthenticated) {
            loginLauncher.launch(Intent(this, SignInActivity::class.java))
        }
    }

    private var avatar: Bitmap? = null
    private lateinit var badge: BadgeDrawable
    private lateinit var appBarConfiguration: AppBarConfiguration

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var requestNotificationsPermission = RequestPermission(
        this, Manifest.permission.POST_NOTIFICATIONS, Build.VERSION_CODES.TIRAMISU
    )

    private var browserClient: CustomTabsClient? = null
    private var browserSession: CustomTabsSession? = null

    private var browserSessionSupported = MutableLiveData<Boolean?>(null)
    private var initialUri: Uri? = null

    private var browserConnection = object : CustomTabsServiceConnection() {
        override fun onServiceDisconnected(name: ComponentName?) {
            browserClient = null
            browserSession = null
        }

        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            client.warmup(0)
            browserSession = client.newSession(CustomTabsCallback())
            browserClient = client
            browserSessionSupported.value = true
        }

    }

    private fun bindCustomTabService(context: Context) {
        // Check for an existing connection
        if (browserClient != null) {
            // Do nothing if there is an existing service connection
            return
        }
        // Get the default browser package name, this will be null if
        // the default browser does not provide a CustomTabsService
        val packageName = CustomTabsClient.getPackageName(context, null)
        packageName?.let {
            CustomTabsClient.bindCustomTabsService(context, it, browserConnection)
        } ?: run {
            browserSessionSupported.value = false
            return
        }
    }


    @ExperimentalBadgeUtils
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ActivityMainBinding.inflate(layoutInflater)
        setContentView(model.root)
        val toolbar = model.toolbar
        //toolbar.inflateMenu(R.menu.toolbar);
        setSupportActionBar(toolbar)

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
            R.id.new_post,
            R.id.discussions
        )
            .build()
        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        //NavigationUI.setupWithNavController(toolbar, navController);
        setupWithNavController(navView, navController)
        navController.addOnDestinationChangedListener { _, destination, args ->
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
            if (id == R.id.blog) {
                val uname = args?.getString("uname") ?: ""
                title = uname.ifEmpty { getString(R.string.Me) }
                supportActionBar?.title = title
            }
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
            if (App.instance.isAuthenticated) {
                navController.navigate(R.id.new_post)
            } else {
                showLogin()
            }
        }
        lifecycleScope.launch {
            if (App.instance.isAuthenticated) {
                if (requestNotificationsPermission()) {
                    notificationManager = NotificationManager()
                }
                refresh()
            }
            Updater(this@MainActivity)
                .checkUpdate()
        }
        account.refresh()
        account.signInStatus.observe(this) { signInStatus: SignInStatus ->
            if (signInStatus == SignInStatus.SIGN_IN_PROGRESS) {
                showLogin()
            }
        }
        badge = BadgeDrawable.create(this)
        badge.isVisible = false
        badge.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
        badge.badgeTextColor = ContextCompat.getColor(this, R.color.colorMainBackground)
        model.toolbar.viewTreeObserver.addOnGlobalLayoutListener {
            BadgeUtils.attachBadgeDrawable(
                badge,
                model.toolbar,
                R.id.discussions
            )
        }
        account.profile.observe(this) { user ->
            when (user) {
                null -> {
                    avatar =
                        ResourcesCompat.getDrawable(resources, R.drawable.av_96, null)!!
                            .toBitmap()
                }

                else -> {
                    val avatarUrl: String = user.avatar
                    val request = ImageRequest.Builder(this@MainActivity)
                        .data(avatarUrl)
                        .target(
                            onStart = {

                            },
                            onSuccess = {
                                avatar = it.toBitmap()
                                invalidateOptionsMenu()
                            },
                            onError = {
                                avatar = null
                                invalidateOptionsMenu()
                            }
                        )
                        .build()
                    this@MainActivity.imageLoader.enqueue(request)
                    if (user.unreadCount > 0) {
                        badge.isVisible = true
                        badge.number = user.unreadCount
                    } else {
                        badge.isVisible = false
                    }
                }
            }
        }
        bindCustomTabService(this)
        browserSessionSupported.observe(this) { supported ->
            when (supported) {
                null -> {}
                else -> {
                    if (supported) {
                        initialUri?.let {
                            openUri(it)
                        }
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
            val msg = intent.getStringExtra(getString(R.string.notification_extra)) ?: ""
            try {
                val jmsg = App.instance.jsonMapper.decodeFromString<Post>(msg)
                if (jmsg.user.uid == 0) {
                    setTitle(R.string.Discussions)
                    //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
                } else {
                    if (jmsg.mid == 0) {
                        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
                        val navController = navHostFragment.navController
                        navController.popBackStack(R.id.chats, true)
                        navController.navigate(R.id.chats)
                        val chatAction = Bundle()
                        chatAction.putString("uname", jmsg.user.uname)
                        chatAction.putInt("uid", jmsg.user.uid)
                        navController.navigate(R.id.PMFragment, chatAction)
                    } else {
                        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
                        val navController = navHostFragment.navController
                        navController.popBackStack(R.id.home, false)
                        val discoverAction = Bundle()
                        discoverAction.putInt("mid", jmsg.mid)
                        discoverAction.putBoolean("scrollToEnd", true)
                        navController.navigate(R.id.thread, discoverAction)
                    }
                }
            } catch (e: IOException) {
                Log.d(this.javaClass.simpleName, "Invalid JSON data", e)
            }
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
        if (action == Intent.ACTION_VIEW) {
            intent.setFlags(0)
            intent.data?.let {
                processUri(it)
            }
            intent.action = ""
        }
    }

    override fun onPause() {
        super.onPause()
        notificationManager?.onPause()
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
                val args = Bundle()
                args.putString("search", s)
                navController.navigate(R.id.search, args)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return true
            }
        })
        val profileItem = menu.findItem(R.id.blog)
        account.profile.value?.let {
            profileItem.isVisible = it.uid > 0
            if (profileItem.isVisible) {
                profileItem.actionView?.setOnClickListener {
                    onOptionsItemSelected(profileItem)
                }
            }
        }
        if (avatar != null) {
            profileItem.actionView?.findViewById<ImageView>(R.id.profile_image)
                ?.setImageBitmap(avatar)
        }
        val discussionsItem = menu.findItem(R.id.discussions)
        if (discussionsItem != null) {
            discussionsItem.actionView?.setOnClickListener {
                onOptionsItemSelected(discussionsItem)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("ObsoleteSdkInt")
    fun processUri(data: Uri) {
        if (data.host == "juick.com") {
            val pathSegments = data.pathSegments
            val navHostFragment = model.navHost.getFragment<Fragment>() as NavHostFragment
            val navController = navHostFragment.navController
            when (pathSegments.size) {
                1 -> {
                    openUri(data)
                }

                2 -> {
                    // thread
                    val threadId = pathSegments[1]
                    val args = Bundle()
                    args.putInt("mid", threadId.toInt())
                    navController.popBackStack(R.id.home, false)
                    navController.navigate(R.id.thread, args)
                }

                else ->                 // discover
                    navController.navigate(R.id.home)
            }
        } else {
            openUri(data)
        }
    }

    private fun openUri(uri: Uri) {
        val handleDeepLinks = uri.host == "juick.com"
        browserSession?.let {
            val intent = CustomTabsIntent.Builder()
                .setSession(it)
                .setSendToExternalDefaultHandlerEnabled(handleDeepLinks)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder().setToolbarColor(
                        ResourcesCompat.getColor(
                            resources, R.color.colorMainBackground, null
                        )
                    ).build()
                ).build()
            try {
                intent.launchUrl(this, uri)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        } ?: run {
            if (browserSessionSupported.value == true) {
                initialUri = uri
            } else {
                openUriFallback(uri)
            }
        }
    }

    private fun openUriFallback(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
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