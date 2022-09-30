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

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.SignInActivity.SignInStatus
import com.juick.android.screens.chats.ChatsFragmentDirections
import com.juick.android.screens.home.HomeFragmentDirections
import com.juick.android.widget.util.ViewUtil
import com.juick.api.model.Post
import com.juick.databinding.ActivityMainBinding
import com.juick.util.StringUtils
import java.io.IOException

/**
 * @author Ugnich Anton
 */
class MainActivity : AppCompatActivity() {
    private var _model: ActivityMainBinding? = null
    private val model get() = _model!!
    private var notificationManager: NotificationManager? = null
    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private fun showLogin() {
        if (!Utils.hasAuth()) {
            loginLauncher.launch(Intent(this, SignInActivity::class.java))
        }
    }

    var appBarConfiguration: AppBarConfiguration? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _model = ActivityMainBinding.inflate(layoutInflater)
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
            R.id.profile,
            R.id.new_post
        )
            .build()
        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController
        setupActionBarWithNavController(this, navController, appBarConfiguration!!)
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
        if (Utils.hasAuth()) {
            notificationManager = NotificationManager()
            val account = Utils.getAccount()
            if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    ), ViewUtil.REQUEST_CODE_SYNC_CONTACTS
                )
            } else {
                ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1)
                ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true)
                ContentResolver.addPeriodicSync(
                    account,
                    ContactsContract.AUTHORITY,
                    Bundle.EMPTY,
                    86400L
                )
            }
            JuickConfig.refresh()
        }
        App.getInstance().setAuthorizationCallback {
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
        App.getInstance().signInStatus.observe(this) { signInStatus: SignInStatus ->
            if (signInStatus == SignInStatus.SIGN_IN_PROGRESS) {
                showLogin()
            }
        }
        model.fab.setOnClickListener {
            if (Utils.hasAuth()) {
                navController.navigate(R.id.new_post)
            } else {
                showLogin()
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
        if (notificationManager != null) {
            notificationManager!!.onResume()
        }
        val intent = intent
        val action = StringUtils.defaultString(intent.action)
        if (action == BuildConfig.INTENT_NEW_EVENT_ACTION) {
            val msg = intent.getStringExtra(getString(R.string.notification_extra))
            try {
                val jmsg = App.getInstance().jsonMapper.readValue(msg, Post::class.java)
                if (jmsg.user.uid == 0) {
                    setTitle(R.string.Discussions)
                    //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getDiscussions()));
                } else {
                    if (jmsg.mid == 0) {
                        val chatAction =
                            ChatsFragmentDirections.actionChatsToPMFragment(jmsg.user.uname)
                        chatAction.uid = jmsg.user.uid
                        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
                        val navController = navHostFragment.navController
                        navController.navigate(chatAction)
                    } else {
                        val discoverAction =
                            HomeFragmentDirections.actionDiscoverFragmentToThreadFragment()
                        discoverAction.mid = jmsg.mid
                        val navHostFragment = model.navHost.getFragment<NavHostFragment>()
                        val navController = navHostFragment.navController
                        navController.navigate(discoverAction)
                    }
                }
            } catch (e: IOException) {
                Log.d(this.javaClass.simpleName, "Invalid JSON data", e)
            }
        }
        if (action == Intent.ACTION_VIEW) {
            val mimeType = StringUtils.defaultString(intent.type)
            if (mimeType == "vnd.android.cursor.item/vnd.com.juick.profile") {
                val contactUri = intent.data
                if (contactUri != null) {
                    val contentResolver = contentResolver
                    val queryResult = contentResolver.query(contactUri, null, null, null, null)
                    if (queryResult != null) {
                        queryResult.moveToFirst()
                        val nameIndex = queryResult.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY)
                        if (nameIndex >= 0) {
                            val name = queryResult.getString(nameIndex)
                            queryResult.close()
                            if (!TextUtils.isEmpty(name)) {
                                title = name
                                //replaceFragment(FeedBuilder.feedFor(UrlBuilder.getUserPostsByName(name)));
                            }
                        }
                    }
                }
            } else {
                val data = intent.data
                data?.let { processUri(it) }
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
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
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
                val action = HomeFragmentDirections.actionDiscoverFragmentToThreadFragment()
                action.mid = threadId.toInt()
                navController.navigate(action)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ViewUtil.REQUEST_CODE_SYNC_CONTACTS) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContentResolver.setSyncAutomatically(
                    Utils.getAccount(),
                    ContactsContract.AUTHORITY,
                    true
                )
                ContentResolver.addPeriodicSync(
                    Utils.getAccount(),
                    ContactsContract.AUTHORITY,
                    Bundle.EMPTY,
                    86400L
                )
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}