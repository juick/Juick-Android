
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
    public static User newInstance(int uid, String uname) {
        User user = new User();
        user.uid = uid;
        user.uname = uname;
        return user;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
