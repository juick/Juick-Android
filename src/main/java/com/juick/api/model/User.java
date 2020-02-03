
package com.juick.api.model;

import com.stfalcon.chatkit.commons.models.IUser;

/**
 * Created by gerc on 10.02.2016.
 */
public class User implements IUser {

    private Integer uid = 0;
    private String uname;
    private String fullname = null;
    private boolean banned;
    private String avatar;

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
}
