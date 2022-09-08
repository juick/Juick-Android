
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

package com.juick.api.model;

import com.stfalcon.chatkit.commons.models.IUser;

import java.util.List;

/**
 * Created by gerc on 10.02.2016.
 */
public class User implements IUser {

    private Integer uid = 0;
    private String uname;
    private String fullname = null;
    private boolean banned;
    private String avatar;
    private int unreadCount;
    private List<User> read;

    public User() {}

    public User(int uid, String uname) {
        this.uid = uid;
        this.uname = uname;
    }

    @Override
    public String getId() {
        return String.valueOf(uid);
    }

    @Override
    public String getName() {
        return uname;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }


    public Integer getUid() {
        return uid;
    }

    public String getUname() {
        return uname;
    }

    public String getFullname() {
        return fullname;
    }

    public boolean isBanned() {
        return banned;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public List<User> getRead() {
        return read;
    }
}
