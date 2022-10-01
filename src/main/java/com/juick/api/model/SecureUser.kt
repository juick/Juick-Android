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
package com.juick.api.model

import com.juick.api.model.Chat
import com.stfalcon.chatkit.commons.models.IDialog
import com.juick.api.model.Post
import com.stfalcon.chatkit.commons.models.IUser
import com.juick.api.model.User
import com.fasterxml.jackson.annotation.JsonFormat
import com.stfalcon.chatkit.commons.models.IMessage
import java.util.ArrayList
import com.fasterxml.jackson.annotation.JsonIgnore
import java.lang.StringBuilder
import com.juick.util.StringUtils

class SecureUser : User() {
    val hash: String? = null
}