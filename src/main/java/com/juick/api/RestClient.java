package com.juick.api;

import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.android.Utils;
import com.juick.api.model.*;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;

import java.util.List;

/**
 * Created by gerc on 14.02.2016.
 */
public class RestClient {

    public static final String ACTION_UPLOAD_PROGRESS = "ACTION_UPLOAD_PROGRESS";
    public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";
    public static final String ACTION_NEW_EVENT = "ACTION_NEW_EVENT";
    public static final String NEW_EVENT_EXTRA = "NEW_EVENT_EXTRA";

    public static String getBaseUrl() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                BuildConfig.API_ENDPOINT : BuildConfig.API_ENDPOINT_FALLBACK;
    }

    public static String getImagesUrl() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                BuildConfig.IMAGES_ENDPOINT : BuildConfig.IMAGES_ENDPOINT_FALLBACK;
    }

    private static ObjectMapper jsonMapper;

    public static ObjectMapper getJsonMapper() {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return jsonMapper;
    }

    private static Api api;

    public static Api getApi() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new UpLoadProgressInterceptor((bytesWritten, contentLength) ->
                            LocalBroadcastManager.getInstance(App.getInstance())
                                    .sendBroadcast(new Intent(ACTION_UPLOAD_PROGRESS)
                                            .putExtra(EXTRA_PROGRESS, (int) (100 * bytesWritten / contentLength)))))
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Authorization", Utils.getBasicAuthString())
                                .header("Accept", "application/json")
                                .method(original.method(), original.body());
                        //Log.e("intercept", requestBuilder.toString());
                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create(getJsonMapper()))
                    .build();

            api = retrofit.create(Api.class);
        }
        return api;
    }

    public static void auth(String username, String password, Callback<Void> callback) {
        String credentials = username + ":" + password;
        final String basic =
                "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", basic)
                            .header("Accept", "application/json")
                            .method(original.method(), original.body());

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(getJsonMapper()))
                .build();
        retrofit.create(Api.class).post(null).enqueue(callback);
    }

    public interface Api {

        @GET("")
        Call<List<Post>> getPosts(@Url String url);

        @GET("/users/friends")
        Call<List<User>> getFriends();

        @GET("/users")
        Call<List<User>> getUsers(@Query("uname") String uname);

        @GET("/pm")
        Call<List<Post>> pm(@Query("uname") String uname);

        @FormUrlEncoded
        @POST("/pm")
        Call<Post> postPm(@Query("uname") String uname, @Field("body") String body);

        @FormUrlEncoded
        @POST("/post")
        Call<Void> post(@Field("body") String body);

        @Multipart
        @POST("/post")
        Call<Void> newPost(@Part("body") RequestBody body, @Part MultipartBody.Part file);

        @GET("")
        Call<List<Tag>> tags(@Url String url);

        @GET("")
        Call<List<Post>> thread(@Url String url);

        @GET("/android/register")
        Call<Void> registerPush(@Query("regid") String login);

        @GET("/groups_pms")
        Call<Pms> groupsPms(@Query("cnt") int cnt);

        @FormUrlEncoded
        @POST("/_google")
        Call<AuthToken> googleAuth(@Field("idToken") String token);
        @FormUrlEncoded
        @POST("/signup")
        Call<Void> signup(@Field("username") String username, @Field("password") String password,
                          @Field("verificationCode") String verificationCode);
    }
}
