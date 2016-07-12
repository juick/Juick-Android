package com.juick.remote.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.List;

/**
 * Created by gerc on 11.03.2016.
 */
@JsonObject
public class Pms {

    @JsonField
    public List<Chat> pms;
}
