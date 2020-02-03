
/*
 * Copyright (C) 2008-2020, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.juick.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by gerc on 10.02.2016.
 */
public class Post implements IMessage {

    private int mid;
    private int replyto;
    private User to;
    private User user;
    private String body;
    private Date timestamp;
    private List<String> tags = new ArrayList<String>();
    private int replies;
    private int likes;
    private String repliesby;
    private String replyQuote;
    private Photo photo;
    private int rid;
    private boolean service;

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
    public User getUser() {
        return user;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @Override
    public Date getCreatedAt() {
        if (timestamp != null) {
            return timestamp;
        }
        return new Date();
    }

    public static Post empty() {

        Post post = new Post();
        post.timestamp = new Date();
        post.body = "";
        return post;
    }

    public boolean isService() {
        return service;
    }

    public void setService(boolean service) {
        this.service = service;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getReplyto() {
        return replyto;
    }

    public void setReplyto(int replyto) {
        this.replyto = replyto;
    }

    public User getTo() {
        return to;
    }

    public void setTo(User to) {
        this.to = to;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getReplies() {
        return replies;
    }

    public void setReplies(int replies) {
        this.replies = replies;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getRepliesby() {
        return repliesby;
    }

    public void setRepliesby(String repliesby) {
        this.repliesby = repliesby;
    }

    public String getReplyQuote() {
        return replyQuote;
    }

    public void setReplyQuote(String replyQuote) {
        this.replyQuote = replyQuote;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public int getRid() {
        return rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
    }
}
