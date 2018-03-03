
package com.juick.api.model;

import android.os.Bundle;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonIgnore;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by gerc on 10.02.2016.
 */
@JsonObject
public class Post implements IMessage {

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
    public int likes;
    @JsonField
    public String repliesby;
    @JsonField
    public String replyQuote;
    @JsonField
    public Photo photo;
    @JsonField
    public Video video;
    @JsonField
    public int rid;

    @JsonIgnore
    public int nextRid;
    @JsonIgnore
    public int prevRid;

    public String getTagsString() {
        StringBuilder builder = new StringBuilder();
        if (!tags.isEmpty()) {
            for (String tag : tags)
                builder.append(" *").append(tag);
        }
        return builder.toString();
    }

    @Override
    public String getId() {
        return String.valueOf(mid);
    }

    @Override
    public String getText() {
        return body;
    }

    @Override
    public IUser getUser() {
        return user;
    }

    @Override
    public Date getCreatedAt() {
        return null;
    }

    public static Post empty() {

        Post post = new Post();
        post.body = "";
        return post;
    }
}
