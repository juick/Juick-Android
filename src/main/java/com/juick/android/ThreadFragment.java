/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.bluelinelabs.logansquare.LoganSquare;
import com.juick.AccountManager;
import com.juick.App;
import com.juick.R;
import com.juick.remote.api.RestClient;
import com.juick.remote.model.Post;
import com.juick.widget.itemanimator.CustomItemAnimator;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadFragment extends BaseFragment implements View.OnClickListener {

    public static final int ACTIVITY_ATTACHMENT_IMAGE = 2;

    public static final String ARG_MID = "ARG_MID";

    TextView tvReplyTo;
    EditText etMessage;
    ImageView bSend;
    ImageView bAttach;
    int rid = 0;
    String attachmentUri = null;
    String attachmentMime = null;
    ProgressDialog progressDialog;
    NewMessageActivity.BooleanReference progressDialogCancel = new NewMessageActivity.BooleanReference(false);
    Handler progressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (progressDialog.getMax() < msg.what) {
                progressDialog.setMax(msg.what);
            } else {
                progressDialog.setProgress(msg.what);
            }
        }
    };

    WebSocket ws;
    int mid = 0;

    RecyclerView recyclerView;
    ProgressBar progressBar;
    JuickMessagesAdapter adapter;

    public ThreadFragment() {
    }

    public static ThreadFragment newInstance(int mid) {
        ThreadFragment fragment = new ThreadFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_MID, mid);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_thread, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mid = args.getInt(ARG_MID, 0);
        }
        if (mid == 0) {
            return;
        }

        tvReplyTo = (TextView) view.findViewById(R.id.textReplyTo);
        etMessage = (EditText) view.findViewById(R.id.editMessage);
        bSend = (ImageView) view.findViewById(R.id.buttonSend);
        bSend.setOnClickListener(this);
        bAttach = (ImageView) view.findViewById(R.id.buttonAttachment);
        bAttach.setOnClickListener(this);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new CustomItemAnimator());
        //recyclerView.setItemAnimator(new ReboundItemAnimator());

        adapter = new JuickMessagesAdapter(true);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new JuickMessagesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Post post = adapter.getItem(position);
                onReply(post.rid, post.body);
            }
        });
        adapter.setOnMenuListener(new JuickMessageMenu(adapter.getItems()));

        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeRefreshLayout.setEnabled(false);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        initWebSocket();
        load();
    }

    private void load() {
        RestClient.getApi().thread("https://api.juick.com/thread?mid=" + mid).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.code() == 404) {
                    Toast.makeText(App.getInstance(), R.string.post_not_found, Toast.LENGTH_LONG).show();
                    return;
                }
                if (adapter.getItemCount() > 0) {
                    initAdapterStageTwo();
                }
                recyclerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                List<Post> list = response.body();
                for (int i = 0; i < list.size(); i++) {
                    int offset = 1;
                    Post commentToCompareWith = list.get(i);
                    for (int j = i + 1; j < list.size(); j++) {
                        Post commentCompared = list.get(j);
                        if (commentCompared.replyto == commentToCompareWith.rid) {
                            list.remove(j);
                            commentCompared.offset = commentToCompareWith.offset + 1;
                            list.add(i + offset++, commentCompared);
                        }
                    }
                }
                adapter.addData(list);
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {

            }
        });
    }

    private void initWebSocket() {
        if (ws != null) return;
        try {
            ws = new WebSocketFactory().createSocket(new URI("wss", "ws.juick.com", "/" + mid, null));
            ws.addHeader("Origin", "ws.juick.com");
            ws.addHeader("Host", "ws.juick.com"); //TODO: remove from server side
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, final String jsonStr) throws Exception {
                    super.onTextMessage(websocket, jsonStr);
                    Log.e("onTextMessage", ""+jsonStr);
                    if (!isAdded()) {
                        return;
                    }
                    ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(250);
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                                try {
                                    adapter.addData(LoganSquare.parse(jsonStr, Post.class));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            });
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        ws.connectAsynchronously();
    }

    private void initAdapterStageTwo() {
        if (!isAdded()) {
            return;
        }
        String replies = getResources().getString(R.string.Replies) + " (" + Integer.toString(adapter.getItemCount() - 1) + ")";
        adapter.addDisabledItem(replies, 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(broadcastReceiver);
    }

    private void resetForm() {
        rid = 0;
        tvReplyTo.setVisibility(View.GONE);
        etMessage.setText("");
        attachmentMime = null;
        attachmentUri = null;
        bAttach.setSelected(false);
        setFormEnabled(true);
    }

    private void setFormEnabled(boolean state) {
        //etMessage.setEnabled(state);
        //bSend.setEnabled(state);
    }

    public void onReply(int newrid, String txt) {
        rid = newrid;
        if (rid > 0) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            String inreplyto = getResources().getString(R.string.In_reply_to_) + " ";
            ssb.append(inreplyto + txt);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, inreplyto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvReplyTo.setText(ssb);
            tvReplyTo.setVisibility(View.VISIBLE);
        } else {
            tvReplyTo.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        if (view == bAttach) {
            if (attachmentUri == null) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_ATTACHMENT_IMAGE);
            } else {
                attachmentUri = null;
                attachmentMime = null;
                bAttach.setSelected(false);
            }
        } else if (view == bSend) {
            if (!AccountManager.hasAuth()) {
                startActivity(new Intent(getContext(), SignInActivity.class));
                return;
            }
            String msg = etMessage.getText().toString();
            if (msg.length() < 3) {
                Toast.makeText(getContext(), R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
                return;
            }
//        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

            String msgnum = "#" + mid;
            if (rid > 0) {
                msgnum += "/" + rid;
            }
            final String body = msgnum + " " + msg;

            setFormEnabled(false);

            if (attachmentUri == null) {
                postText(body);
            } else {
                postMedia(body);
            }
        }
    }

    public void postText(final String body) {
        RestClient.getApi().post(body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response != null) {
                    Toast.makeText(App.getInstance(), R.string.Message_posted, Toast.LENGTH_SHORT).show();
                    resetForm();
                } else {
                    Toast.makeText(App.getInstance(), R.string.Error, Toast.LENGTH_SHORT).show();
                    setFormEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {

            }
        });
    }

    public void postMedia(final String body) {
        progressDialog = new ProgressDialog(getContext());
        progressDialogCancel.bool = false;
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                progressDialogCancel.bool = true;
            }
        });
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(0);
        progressDialog.show();
        new Thread(new Runnable() {

            public void run() {
                final boolean res = NewMessageActivity.sendMessage(getActivity(), body, 0, 0, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel);
                getActivity().runOnUiThread(new Runnable() {

                    public void run() {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        setFormEnabled(true);
                        if (res) {
                            resetForm();
                        }
                        if (res && attachmentUri == null) {
                            Toast.makeText(App.getInstance(), R.string.Message_posted, Toast.LENGTH_LONG).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setNeutralButton(android.R.string.ok, null);
                            if (res) {
                                builder.setIcon(android.R.drawable.ic_dialog_info);
                                builder.setMessage(R.string.Message_posted);
                            } else {
                                builder.setIcon(android.R.drawable.ic_dialog_alert);
                                builder.setMessage(R.string.Error);
                            }
                            builder.show();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == BaseActivity.RESULT_OK) {
            if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = data.getDataString();
                // How to get correct mime type?
                attachmentMime = "image/jpeg";
                bAttach.setSelected(true);
            }
        }
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RestClient.ACTION_UPLOAD_PROGRESS)) {
                if (progressDialog != null) {
                    progressHandler.sendEmptyMessage(intent.getIntExtra(RestClient.EXTRA_PROGRESS, 0));
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(com.juick.android.TagsFragment.TAG_SELECT_ACTION));
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(broadcastReceiver, new IntentFilter(RestClient.ACTION_UPLOAD_PROGRESS));
    }
}
