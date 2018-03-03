
package com.juick.api.model;

import android.os.Bundle;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.juick.api.RestClient;
import com.stfalcon.chatkit.commons.models.IUser;

/**
 * Created by gerc on 10.02.2016.
 */
@JsonObject
public class User implements IUser {

    @JsonField
    public Integer uid;
    @JsonField
    public String uname;
    @JsonField
    public String fullname = null;
    @JsonField
    public boolean banned;

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
        return RestClient.getImagesUrl() + "a/" + uid + ".png";
    }
    public static User newInstance(int uid, String uname) {
        User user = new User();
        user.uid = uid;
        user.uname = uname;
        return user;
    }
}
