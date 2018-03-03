package com.juick.android;
import android.os.Parcel;
import android.os.Parcelable;

import com.juick.api.RestClient;

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

    public static String getNormalPostById(String mid) {
        return RestClient.getBaseUrl() + mid;
    }
}
