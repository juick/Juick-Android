/*
 * Copyright (C) 2008-2020, Juick
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

import android.content.ContentResolver
import android.os.Bundle
import com.juick.App
import com.juick.R
import com.juick.android.Utils.account

object JuickConfig {
    fun init() {
        //App.instance.signInProvider = { context, button -> null }
    }

    fun refresh() {
        val messagesProviderAuthority: String =
            App.instance.getString(R.string.messages_provider_authority)
        val account = account
        ContentResolver.setIsSyncable(account, messagesProviderAuthority, 1)
        ContentResolver.setSyncAutomatically(account, messagesProviderAuthority, true)
        ContentResolver.addPeriodicSync(account, messagesProviderAuthority, Bundle.EMPTY, 300L)
    }
}