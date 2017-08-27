package com.juick.api;

import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.android.Utils;
import com.juick.api.model.Pms;
import com.juick.api.model.Post;
import com.juick.api.model.Tag;
import com.juick.api.model.User;
import com.github.aurae.retrofit2.LoganSquareConverterFactory;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by gerc on 14.02.2016.
 */
public class RestClient {

    public static final String ACTION_UPLOAD_PROGRESS = "ACTION_UPLOAD_PROGRESS";
    public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";

    private static Api api;

    public static Api getApi() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new UpLoadProgressInterceptor(new CountingRequestBody.Listener() {
                        @Override
                        public void onRequestProgress(long bytesWritten, long contentLength) {
                            LocalBroadcastManager.getInstance(App.getInstance())
                                    .sendBroadcast(new Intent(ACTION_UPLOAD_PROGRESS)
                                            .putExtra(EXTRA_PROGRESS, (int)(100 * bytesWritten / contentLength)));
                        }
                    }))
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Interceptor.Chain chain) throws IOException {
                            Request original = chain.request();

                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("Authorization", Utils.getBasicAuthString())
                                    .header("Accept", "application/json")
                                    .method(original.method(), original.body());
                            //Log.e("intercept", requestBuilder.toString());
                            Request request = requestBuilder.build();
                            return chain.proceed(request);
                        }
                    })
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                            BuildConfig.API_ENDPOINT : BuildConfig.API_ENDPOINT_FALLBACK)
                    .client(client)
                    .addConverterFactory(LoganSquareConverterFactory.create())
                    .build();

            api = retrofit.create(Api.class);
        }
        return api;
    }

    public static void auth(String username, String password, Callback<Object> callback) {
        String credentials = username + ":" + password;
        final String basic =
                "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Request original = chain.request();

                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Authorization", basic)
                                .header("Accept", "application/json")
                                .method(original.method(), original.body());

                        Request request = requestBuilder.build();
                        return chain.proceed(request);
                    }
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                        BuildConfig.API_ENDPOINT : BuildConfig.API_ENDPOINT_FALLBACK)
                .client(client)
                .addConverterFactory(LoganSquareConverterFactory.create())
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
        Call<Object> post(@Field("body") String body);

        @Multipart
        @POST("/post")
        Call<String> newPost(@Part("body") RequestBody body,
                             @Part("lat") RequestBody lat, @Part("lon") RequestBody lon,
                             @Part MultipartBody.Part file);

        @GET("")
        Call<List<Tag>> tags(@Url String url);

        @GET("")
        Call<List<Post>> thread(@Url String url);

        @GET("/android/register")
        Call<String> registerPush(@Query("regid") String login);

        @GET("/android/unregister")
        Call<String> unregisterPush(@Query("regid") String login);

        @GET("/groups_pms")
        Call<Pms> groupsPms(@Query("cnt") int cnt);
    }
}
