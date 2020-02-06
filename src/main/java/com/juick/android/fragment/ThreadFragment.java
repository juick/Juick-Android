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
package com.juick.android.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.juick.App;
import com.juick.R;
import com.juick.android.JuickMessageMenu;
import com.juick.android.JuickMessagesAdapter;
import com.juick.android.NewMessageActivity;
import com.juick.android.SignInActivity;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.RestClient;
import com.juick.api.model.Post;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadFragment extends BaseFragment implements View.OnClickListener {

    private static final int ACTIVITY_ATTACHMENT_IMAGE = 2;

    private static final String ARG_MID = "ARG_MID";

    private TextView tvReplyTo;
    private EditText etMessage;
    private ImageView bSend;
    private ImageView bAttach;
    private int rid = 0;
    private String attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog;
    private NewMessageActivity.BooleanReference progressDialogCancel = new NewMessageActivity.BooleanReference(false);
    private Handler progressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (progressDialog.getMax() < msg.what) {
                progressDialog.setMax(msg.what);
            } else {
                progressDialog.setProgress(msg.what);
            }
        }
    };

    private int mid = 0;

    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar progressBar;
    private JuickMessagesAdapter adapter;

    public static ThreadFragment newInstance(int mid) {
        ThreadFragment fragment = new ThreadFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_MID, mid);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_thread, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mid = args.getInt(ARG_MID, 0);
        }
        if (mid == 0) {
            return;
        }

        tvReplyTo = view.findViewById(R.id.textReplyTo);
        etMessage = view.findViewById(R.id.editMessage);
        bSend = view.findViewById(R.id.buttonSend);
        bSend.setOnClickListener(this);
        bAttach = view.findViewById(R.id.buttonAttachment);
        bAttach.setOnClickListener(this);

        progressBar = view.findViewById(R.id.progressBar);

        recyclerView = view.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);

        adapter = new JuickMessagesAdapter();
        recyclerView.setAdapter(adapter);
        linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        adapter.setOnItemClickListener((view1, position) -> {
            Post post = adapter.getItem(position);
            onReply(post.getRid(), post.getBody());
        });
        adapter.setOnMenuListener(new JuickMessageMenu(adapter.getItems()));
        adapter.setOnScrollListener((v, replyTo, rid) -> {
            int pos = 0;
            for (int i = 0; i < adapter.getItems().size(); ++i) {
                Post p = adapter.getItems().get(i);
                if (p.getRid() == replyTo) {
                    p.nextRid = replyTo;
                    if (p.prevRid == 0)
                        p.prevRid = rid;
                    pos = i;
                    break;
                }
            }
            if (pos != 0) {
                adapter.notifyItemChanged(pos);
                recyclerView.scrollToPosition(pos);
            }
        });

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_container);
        swipeRefreshLayout.setEnabled(false);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        load();
    }

    @Override
    public void reload(){
        super.reload();
        load();
    }

    private void load() {
        RestClient.getApi().thread(mid)
                .enqueue(new Callback<List<Post>>() {
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
                        adapter.newData(list);
                    }

                    @Override
                    public void onFailure(Call<List<Post>> call, Throwable t) {
                        Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void initAdapterStageTwo() {
        if (!isAdded()) {
            return;
        }
        String replies = getResources().getString(R.string.Replies) + " (" + (adapter.getItemCount() - 1) + ")";
        adapter.addDisabledItem(replies, 1);
    }

    @Override
    public void onPause() {
        super.onPause();
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
        etMessage.setEnabled(state);
        bSend.setEnabled(state);
    }

    private void onReply(int newrid, String txt) {
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
            if (Build.VERSION.SDK_INT >= 23 && getBaseActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ViewUtil.REQUEST_CODE_READ_EXTERNAL_STORAGE);
                return;
            }
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
            if (!Utils.hasAuth()) {
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

    private void postText(final String body) {
        RestClient.getApi().post(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && isAdded()) {
                    Toast.makeText(App.getInstance(), R.string.Message_posted, Toast.LENGTH_SHORT).show();
                    resetForm();
                    ViewUtil.hideKeyboard(getActivity());
                } else {
                    Toast.makeText(App.getInstance(), R.string.Error, Toast.LENGTH_SHORT).show();
                    setFormEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                resetForm();
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void postMedia(final String body) {
        progressDialog = new ProgressDialog(getContext());
        progressDialogCancel.bool = false;
        progressDialog.setOnCancelListener(arg0 -> progressDialogCancel.bool = true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(0);
        progressDialog.show();
        new Thread(() -> {
            final boolean res = NewMessageActivity.sendMessage(getActivity(), body, attachmentUri, attachmentMime, progressDialog, progressHandler, progressDialogCancel);
            getActivity().runOnUiThread(() -> {
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
            });
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = Utils.getPath(Uri.parse(data.getDataString()));
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
            if (intent.getAction().equals(RestClient.ACTION_NEW_EVENT)) {
                if (!isAdded()) {
                    return;
                }
                String data = intent.getStringExtra(RestClient.NEW_EVENT_EXTRA);
                if (data != null && !data.trim().isEmpty()) {
                    try {
                        final Post reply = RestClient.getJsonMapper().readValue(data, Post.class);
                        getActivity().runOnUiThread(() -> {
                            if (adapter.getItemCount() > 0) {
                                if (adapter.getItem(0).getMid() == reply.getMid()) {
                                    adapter.addData(reply);
                                    linearLayoutManager.smoothScrollToPosition(recyclerView,
                                            new RecyclerView.State(), reply.getRid());
                                }
                            }
                        });
                    } catch (IOException e) {
                        Log.d("SSE", e.getLocalizedMessage());
                    }
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(TagsFragment.TAG_SELECT_ACTION));
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(broadcastReceiver, new IntentFilter(RestClient.ACTION_UPLOAD_PROGRESS));
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(broadcastReceiver, new IntentFilter(RestClient.ACTION_NEW_EVENT));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == ViewUtil.REQUEST_CODE_READ_EXTERNAL_STORAGE) {
                bAttach.performClick();
            }
        }
    }
}
