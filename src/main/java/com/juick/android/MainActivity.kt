/*
 * Copyright (C) 2008-2026, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package com.juick.android

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.SignInActivity.SignInStatus
import com.juick.android.service.isAuthenticated
import com.juick.android.updater.Updater
import androidx.navigation.compose.rememberNavController
import com.juick.android.ui.AppTheme
import com.juick.android.ui.navigation.AppNavigation
import com.juick.android.ui.navigation.Route
import com.juick.api.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val account by viewModels<Account>()
    private var notificationManager: NotificationManager? = null
    private lateinit var loginLauncher: ActivityResultLauncher<Intent>

    private fun showLogin() {
        if (!App.instance.isAuthenticated) {
            loginLauncher.launch(Intent(this, SignInActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var requestNotificationsPermission = RequestPermission(
        this, Manifest.permission.POST_NOTIFICATIONS, Build.VERSION_CODES.TIRAMISU
    )

    private var browserClient: CustomTabsClient? = null
    private var browserSession: CustomTabsSession? = null
    private var browserSessionSupported = MutableLiveData<Boolean?>(null)
    private var customTabsBound = false
    private var initialUri: Uri? = null

    private var browserConnection = object : CustomTabsServiceConnection() {
        override fun onServiceDisconnected(name: ComponentName?) {
            browserClient = null
            browserSession = null
            customTabsBound = false
        }
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            client.warmup(0)
            browserSession = client.newSession(CustomTabsCallback())
            browserClient = client
            browserSessionSupported.value = true
        }
    }

    private fun bindCustomTabService(context: Context) {
        if (customTabsBound) return
        val packageName = CustomTabsClient.getPackageName(context, null)
        packageName?.let {
            customTabsBound = CustomTabsClient.bindCustomTabsService(context, it, browserConnection)
        }
    }

    private fun openUri(uri: Uri) {
        try {
            val colorScheme = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(getColor(R.color.colorMainBackground))
                .build()
            val builder = CustomTabsIntent.Builder()
                .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_SYSTEM, colorScheme)
                .setSendToExternalDefaultHandlerEnabled(true)
            browserSession?.let { builder.setSession(it) }
            builder.build().launchUrl(this, uri)
        } catch (e: Exception) {
            openUriFallback(uri)
        }
    }

    private fun openUriFallback(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Log.e("MainActivity", "Cannot open URL: $uri", e)
        }
    }

    fun processUri(data: Uri): Boolean {
        val path = data.path ?: return false
        val segments = path.split("/").filter { it.isNotEmpty() }
        when (segments.size) {
            0 -> {
                // home
            }
            1 -> {
                // user profile → navigate in-app
                navController?.navigate(Route.Blog(segments[0]))
                return true
            }
            2 -> {
                if (segments[0] == "m") {
                    val mid = segments[1].toIntOrNull() ?: return false
                    // Navigate to thread via callback
                    processUriCallback?.invoke(mid)
                    return true
                }
            }
            else -> {
                if (segments.size > 2 && segments[0] == "i") {
                    openUri(data)
                    return true
                }
            }
        }
        return false
    }

    internal var processUriCallback: ((Int) -> Unit)? = null
    private var navController: androidx.navigation.NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                account.refresh()
            }
        }

        bindCustomTabService(this)

        account.signInStatus.observe(this) { status ->
            if (status == SignInStatus.SIGN_IN_PROGRESS) {
                showLogin()
            }
        }

        App.instance.authorizationCallback = {
            val intent = Intent(this, SignInActivity::class.java).apply {
                putExtra(SignInActivity.EXTRA_ACTION, SignInActivity.ACTION_PASSWORD_UPDATE)
            }
            startActivity(intent)
        }

        account.refresh()

        lifecycleScope.launch {
            if (BuildConfig.ENABLE_UPDATER) {
                Updater(this@MainActivity).checkUpdate()
            }
            try {
                requestNotificationsPermission()
                notificationManager = NotificationManager()
            } catch (_: Exception) { }
        }

        initialUri = intent?.data

        setContent {
            AppTheme {
                val navController = rememberNavController()
                this@MainActivity.navController = navController
                processUriCallback = { mid ->
                    navController.navigate(Route.Thread(mid))
                }

                val profile by account.profile.observeAsState()
                val unreadCount = profile?.unreadCount ?: 0

                val onPostClick: (Post) -> Unit = { post -> navController.navigate(Route.Thread(post.mid)) }
                val onUserClick: (String) -> Unit = { uname -> navController.navigate(Route.Blog(uname)) }
                val onLinkClick: (String) -> Unit = { url -> openUri(Uri.parse(url)) }
                val onSignInClick: () -> Unit = { showLogin() }
                val onLikeClick: (Post) -> Unit = { post ->
                    lifecycleScope.launch {
                        try { App.instance.api.like(post.mid); account.refresh() } catch (_: Exception) { }
                    }
                }
                val onMenuClick: (Post) -> Unit = { }
                val onFabClick: () -> Unit = {
                    if (App.instance.isAuthenticated) navController.navigate(Route.NewPost()) else showLogin()
                }

                AppNavigation(navController, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, onSignInClick, onFabClick, profile, unreadCount, App.instance.isAuthenticated)

                LaunchedEffect(Unit) {
                    initialUri?.let { processUri(it); initialUri = null }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (Intent.ACTION_VIEW == it.action) {
                it.data?.let { data ->
                    processUri(data)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        notificationManager?.onResume()
        account.refresh()
        val intent = intent
        if (Intent.ACTION_SEND == intent.action) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (text.isNotEmpty() || stream != null) {
                intent.action = null
                val uriParam = stream?.let { Uri.encode(it.toString()) } ?: ""
                navController?.navigate(Route.NewPost(text = Uri.encode(text), uri = uriParam))
            }
        }
        if (BuildConfig.INTENT_NEW_EVENT_ACTION == intent.action) {
            handleNewEventIntent(intent)
        }
    }

    override fun onPause() {
        notificationManager?.onPause()
        super.onPause()
    }

    private fun handleNewEventIntent(intent: Intent) {
        intent.action = null
        val msg = intent.getStringExtra(getString(R.string.notification_extra)) ?: return
        try {
            val post = App.instance.jsonMapper.decodeFromString<Post>(msg)
            if (post.mid > 0) {
                navController?.navigate(Route.Thread(post.mid))
            }
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        if (customTabsBound) {
            unbindService(browserConnection)
            customTabsBound = false
        }
        browserClient = null
        browserSession = null
        super.onDestroy()
    }
}
