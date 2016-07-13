
package com.juick.api.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by gerc on 10.02.2016.
 */
@JsonObject
public class User {

    @JsonField
    public Integer uid;
    @JsonField
    public String uname;
    @JsonField
    public String fullname = null;
}
