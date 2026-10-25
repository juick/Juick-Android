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

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.SignInActivity.SignInStatus
import com.juick.android.service.isAuthenticated
import com.juick.android.ui.AppTheme
import com.juick.android.ui.navigation.AppNavigation
import com.juick.android.ui.navigation.Route
import com.juick.android.ui.screens.feed.LikeConfirmDialog
import com.juick.android.updater.Updater
import com.juick.api.model.Post
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val LIKE_REACTION_ID = 1

class MainActivity : ComponentActivity() {
    val account by viewModels<Account>()
    private var notificationManager: NotificationManager? = null
    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private lateinit var passwordUpdateLauncher: ActivityResultLauncher<Intent>
    private val passwordUpdateShown = AtomicBoolean(false)

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
    private var customTabsBound = false
    private var navController: NavHostController? = null

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
            val handleDeepLinks = uri.host == "juick.com"
            val builder = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorScheme)
                .setSendToExternalDefaultHandlerEnabled(handleDeepLinks)
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

    fun processUri(data: Uri) {
        val nav = navController
        val isJuickLink = data.host == "juick.com"
        if (nav == null || !isJuickLink) {
            openUri(data)
            return
        }
        val segments = data.pathSegments
        val isRootPath = segments.isEmpty()
        val isUserPath = segments.size == 1
        val threadId = segments.getOrNull(1)?.toIntOrNull() ?: 0
        val isThreadPath = segments.size == 2 && threadId > 0
        val isImagePath = segments.firstOrNull() == "i"
        when {
            isRootPath -> nav.navigate(Route.Home)
            isUserPath -> nav.navigate(Route.Blog(segments[0]))
            isThreadPath -> nav.navigate(Route.Thread(threadId))
            isImagePath -> openUri(data)
            else -> nav.navigate(Route.Home)
        }
    }

    private fun onSignedIn() {
        account.refresh(force = true)
        navController?.navigate(Route.Home) { popUpTo(0) { inclusive = true } }
    }

    private fun initNotifications() {
        if (notificationManager != null || !App.instance.isAuthenticated) return
        lifecycleScope.launch {
            if (requestNotificationsPermission()) {
                notificationManager = NotificationManager()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // the restored back stack already reflects the launch intent
            intent.action = null
        }

        loginLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                onSignedIn()
                initNotifications()
            }
        }

        passwordUpdateLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            passwordUpdateShown.set(false)
            if (result.resultCode == RESULT_OK) {
                onSignedIn()
            }
        }

        bindCustomTabService(this)

        account.signInStatus.observe(this) { status ->
            if (status == SignInStatus.SIGN_IN_PROGRESS) {
                showLogin()
            }
        }

        App.instance.authorizationCallback = {
            if (passwordUpdateShown.compareAndSet(false, true)) {
                runOnUiThread {
                    val intent = Intent(this, SignInActivity::class.java).apply {
                        putExtra(SignInActivity.EXTRA_ACTION, SignInActivity.ACTION_PASSWORD_UPDATE)
                    }
                    passwordUpdateLauncher.launch(intent)
                }
            }
        }

        account.refresh()
        initNotifications()

        if (BuildConfig.ENABLE_UPDATER) {
            lifecycleScope.launch {
                Updater(this@MainActivity).checkUpdate()
            }
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                this@MainActivity.navController = navController

                val profile by account.profile.observeAsState()
                val unreadCount = profile?.unreadCount ?: 0

                val onPostClick: (Post) -> Unit = { post -> navController.navigate(Route.Thread(post.mid)) }
                val onUserClick: (String) -> Unit = { uname -> navController.navigate(Route.Blog(uname)) }
                val onLinkClick: (String) -> Unit = { url -> processUri(Uri.parse(url)) }
                val onSignInClick: () -> Unit = { showLogin() }
                var likeConfirmPost by remember { mutableStateOf<Post?>(null) }
                val onLikeClick: (Post) -> Unit = { post -> likeConfirmPost = post }
                val onLikeConfirmed: (Post) -> Unit = { post ->
                    lifecycleScope.launch {
                        try {
                            val updated = if (post.liked) App.instance.api.like(post.mid) else App.instance.api.react(post.mid, LIKE_REACTION_ID)
                            App.instance.postUpdates.emit(updated)
                            account.refresh(force = true)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "like failed", e)
                        }
                    }
                }
                val onMenuClick: (Post) -> Unit = { }
                val onFabClick: () -> Unit = {
                    if (App.instance.isAuthenticated) navController.navigate(Route.NewPost()) else showLogin()
                }

                AppNavigation(navController, onPostClick, onUserClick, onMenuClick, onLikeClick, onLinkClick, onSignInClick, onFabClick, profile, unreadCount, App.instance.isAuthenticated, onProfileChanged = { account.refresh(force = true) })

                likeConfirmPost?.let { post ->
                    LikeConfirmDialog(
                        post = post,
                        onConfirm = {
                            likeConfirmPost = null
                            onLikeConfirmed(post)
                        },
                        onDismiss = { likeConfirmPost = null },
                    )
                }

                // onResume runs before the first composition, so a cold-start intent is handled here
                LaunchedEffect(Unit) { handleIntent() }
            }
        }
    }

    private fun handleIntent() {
        val nav = navController ?: return
        val intent = intent
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { processUri(it) }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (!text.isNullOrEmpty() || stream != null) {
                    nav.navigate(Route.NewPost(text = text, uri = stream?.toString()))
                }
            }
            BuildConfig.INTENT_NEW_EVENT_ACTION -> handleNewEventIntent(nav, intent)
            else -> return
        }
        intent.action = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        notificationManager?.onResume()
        account.refresh()
        handleIntent()
    }

    override fun onPause() {
        notificationManager?.onPause()
        super.onPause()
    }

    private fun handleNewEventIntent(nav: NavHostController, intent: Intent) {
        val msg = intent.getStringExtra(getString(R.string.notification_extra)) ?: return
        try {
            val post = App.instance.jsonMapper.decodeFromString<Post>(msg)
            when {
                post.user.uid == 0 -> nav.navigate(Route.Discussions)
                post.mid == 0 -> nav.navigate(Route.Chat(post.user.uname, post.user.uid))
                else -> nav.navigate(Route.Thread(post.mid, scrollToEnd = true))
            }
        } catch (e: Exception) {
            Log.d("MainActivity", "Invalid notification data", e)
        }
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
