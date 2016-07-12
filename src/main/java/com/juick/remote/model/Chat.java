package com.juick.remote.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by gerc on 11.03.2016.
 */
@JsonObject
public class Chat {

    @JsonField
    public String uname;
    @JsonField
    public int uid;
    @JsonField
    public int MessagesCount;
}
