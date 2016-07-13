
package com.juick.api.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by gerc on 10.02.2016.
 */
@JsonObject
public class Photo {

    @JsonField
    public String thumbnail;
    @JsonField
    public String small;
    @JsonField
    public String medium;

}
