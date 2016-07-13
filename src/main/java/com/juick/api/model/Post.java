
package com.juick.api.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gerc on 10.02.2016.
 */
@JsonObject
public class Post {

    @JsonField
    public int mid;
    @JsonField
    public int replyto;
    @JsonField
    public User user;
    @JsonField
    public String body;
    @JsonField
    public String timestamp;
    @JsonField
    public List<String> tags = new ArrayList<String>();
    @JsonField
    public int replies;
    @JsonField
    public String repliesby;
    @JsonField
    public Photo photo;
    @JsonField
    public Video video;
    @JsonField
    public int rid;

    public int offset;
}
