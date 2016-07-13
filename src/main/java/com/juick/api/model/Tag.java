package com.juick.api.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by gerc on 15.02.2016.
 */
@JsonObject
public class Tag {

    @JsonField
    public String tag;
}
