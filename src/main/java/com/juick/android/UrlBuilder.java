/*
 * Copyright (C) 2008-2021, Juick
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

package com.juick.android;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
/**
 * Created by alx on 11.12.16.
 */

public class UrlBuilder implements Parcelable {
    String url;

    private UrlBuilder(){}
    private UrlBuilder(String u) { url = u;}

    public static UrlBuilder goHome(){
        UrlBuilder url = new UrlBuilder();
        url.url = "/home?1=1";
        return url;
    }

    public static UrlBuilder getPhotos(){
        UrlBuilder url = new UrlBuilder();
        url.url = "messages?media=all";
        return url;
    }

    public static UrlBuilder getLast(){
        UrlBuilder url = new UrlBuilder();
        url.url = "messages?1=1";
        return url;
    }

    public static UrlBuilder getTop(){
        UrlBuilder url = new UrlBuilder();
        url.url = "messages?popular=1";
        return url;
    }

    public static UrlBuilder getUserPostsByName(String uname){
        UrlBuilder url = new UrlBuilder();
        url.url = "messages?uname=" + uname;
        return url;
    }

    public static UrlBuilder getUserPostsByUid(Integer uid){
        UrlBuilder url = new UrlBuilder();
        url.url = "messages?uid=" + uid;
        return url;
    }

    public static UrlBuilder getPostsByTag(int uid, String tag) {
        UrlBuilder url = new UrlBuilder();
        try {
            url.url = "messages?tag=" + URLEncoder.encode(tag, "utf-8");
        }catch (UnsupportedEncodingException ex){

        }
        return url;
    }
    public static UrlBuilder getDiscussions() {
        UrlBuilder builder = new UrlBuilder();
        builder.url = "messages/discussions";
        return builder;
    }

    @Override
    public String toString() {
        return url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
    }

    public static final Parcelable.Creator<UrlBuilder> CREATOR = new Parcelable.Creator<UrlBuilder>() {
        public UrlBuilder createFromParcel(Parcel in) {
            return new UrlBuilder(in.readString());
        }
        public UrlBuilder[] newArray(int size) {
            return new UrlBuilder[size];
        }
    };
}
