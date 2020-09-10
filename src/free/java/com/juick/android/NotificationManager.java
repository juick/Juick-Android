package com.juick.android;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NotificationManager {

    private Context context;

    private OkHttpClient es;

    public NotificationManager(Context context) {
        this.context = context;
    }
    public void onResume() {
        if (es != null) return;
        es = Utils.getSSEFactory()
                .readTimeout(0, TimeUnit.SECONDS).build();
        Request request = new Request.Builder()
                .url(BuildConfig.EVENTS_ENDPOINT)
                .build();
        OkSse sse = new OkSse(es);
        sse.newServerSentEvent(request, new ServerSentEvent.Listener() {
            @Override
            public void onOpen(ServerSentEvent sse, okhttp3.Response response) {
                Log.d("SSE", "Event listener opened");
            }

            @Override
            public void onMessage(ServerSentEvent sse, String id, String event, String message) {
                Log.d("SSE", "event received: " + event);
                if (event.equals("msg")) {
                    LocalBroadcastManager.getInstance(App.getInstance())
                            .sendBroadcast(new Intent(BuildConfig.INTENT_NEW_EVENT_ACTION)
                                    .putExtra(context.getString(R.string.notification_extra), message));
                }
            }

            @Override
            public void onComment(ServerSentEvent sse, String comment) {

            }

            @Override
            public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
                return true;
            }

            @Override
            public boolean onRetryError(ServerSentEvent sse, Throwable throwable, okhttp3.Response response) {
                return true;
            }

            @Override
            public void onClosed(ServerSentEvent sse) {
            }

            @Override
            public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
                return originalRequest;
            }
        });
    }
}
