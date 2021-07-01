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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.JuickMessageMenu;
import com.juick.android.JuickMessagesAdapter;
import com.juick.android.NewMessageActivity;
import com.juick.android.SignInActivity;
import com.juick.android.Utils;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.model.Post;
import com.juick.databinding.FragmentThreadBinding;
import com.juick.util.StringUtils;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadFragment extends BaseFragment {

    private FragmentThreadBinding model;

    private static final int ACTIVITY_ATTACHMENT_IMAGE = 2;

    private static final String ARG_MID = "ARG_MID";

    private int rid = 0;
    private String attachmentUri = null;
    private String attachmentMime = null;
    private ProgressDialog progressDialog;
    private NewMessageActivity.BooleanReference progressDialogCancel = new NewMessageActivity.BooleanReference(false);

    private int mid = 0;

    private LinearLayoutManager linearLayoutManager;
    private JuickMessagesAdapter adapter;

    public static ThreadFragment newInstance(int mid) {
        ThreadFragment fragment = new ThreadFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_MID, mid);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new JuickMessagesAdapter();
    }

    public ThreadFragment() {
        super(R.layout.fragment_thread);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        model = FragmentThreadBinding.bind(view);

        Bundle args = getArguments();
        if (args != null) {
            mid = args.getInt(ARG_MID, 0);
        }
        if (mid == 0) {
            return;
        }
        model.buttonSend.setOnClickListener(v -> {
            if (!Utils.hasAuth()) {
                startActivity(new Intent(getContext(), SignInActivity.class));
                return;
            }
            String msg = model.editMessage.getText().toString();
            if (msg.length() < 3) {
                Toast.makeText(getContext(), R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
                return;
            }

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
        });

        model.buttonAttachment.setOnClickListener(v -> {
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
                model.buttonAttachment.setSelected(false);
            }
        });

        model.list.setAdapter(adapter);
        linearLayoutManager = (LinearLayoutManager) model.list.getLayoutManager();
        adapter.setOnItemClickListener((widget, position) -> {
            if (widget.getTag() == null || !widget.getTag().equals("clicked")) {
                Post post = adapter.getItem(position);
                onReply(post.getRid(), StringUtils.defaultString(post.getBody()));
            }
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
                model.list.scrollToPosition(pos);
            }
        });

        model.swipeContainer.setEnabled(false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        model.list.setVisibility(View.GONE);
        model.progressBar.setVisibility(View.VISIBLE);
        load();
    }

    @Override
    public void reload(){
        super.reload();
        load();
    }

    private void load() {
        App.getInstance().getApi().thread(mid)
                .enqueue(new Callback<List<Post>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Post>> call, @NonNull Response<List<Post>> response) {
                        if (response.isSuccessful() && isAdded()) {
                            if (response.code() == 404) {
                                Toast.makeText(App.getInstance(), R.string.post_not_found, Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (adapter.getItemCount() > 0) {
                                initAdapterStageTwo();
                            }
                            model.list.setVisibility(View.VISIBLE);
                            model.progressBar.setVisibility(View.GONE);
                            List<Post> list = response.body();
                            adapter.newData(list);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                        if (isAdded()) {
                            Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                        }
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
        LocalBroadcastManager.getInstance(App.getInstance()).unregisterReceiver(broadcastReceiver);
    }

    private void resetForm() {
        rid = 0;
        model.textReplyTo.setVisibility(View.GONE);
        model.editMessage.setText(StringUtils.EMPTY);
        attachmentMime = null;
        attachmentUri = null;
        model.buttonAttachment.setSelected(false);
        setFormEnabled(true);
    }

    private void setFormEnabled(boolean state) {
        model.editMessage.setEnabled(state);
        model.buttonSend.setEnabled(state);
    }

    private void onReply(int newrid, String txt) {
        rid = newrid;
        if (rid > 0) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            String inreplyto = getResources().getString(R.string.In_reply_to_) + " ";
            ssb.append(inreplyto).append(txt);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, inreplyto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            model.textReplyTo.setText(ssb);
            model.textReplyTo.setVisibility(View.VISIBLE);
        } else {
            model.textReplyTo.setVisibility(View.GONE);
        }
    }

    private void postText(final String body) {
        App.getInstance().getApi().post(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
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
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
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
        App.getInstance().setOnProgressListener(progress -> {
            if (progressDialog.getMax() < progress) {
                progressDialog.setMax((int)progress);
            } else {
                progressDialog.setProgress((int)progress);
            }
        });
        App.getInstance().sendMessage(body, attachmentUri, attachmentMime, (success) -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            setFormEnabled(true);
            if (success) {
                resetForm();
            }
            if (success && attachmentUri == null) {
                Toast.makeText(App.getInstance(), R.string.Message_posted, Toast.LENGTH_LONG).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setNeutralButton(android.R.string.ok, null);
                if (success) {
                    builder.setIcon(android.R.drawable.ic_dialog_info);
                    builder.setMessage(R.string.Message_posted);
                } else {
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(R.string.Error);
                }
                builder.show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ACTIVITY_ATTACHMENT_IMAGE && data != null) {
                attachmentUri = Utils.getPath(Uri.parse(data.getDataString()));
                if (TextUtils.isEmpty(attachmentUri)) {
                    Toast.makeText(getActivity(), R.string.error_unsupported_content, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // How to get correct mime type?
                    attachmentMime = "image/jpeg";
                    model.buttonAttachment.setSelected(true);
                }
            }
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.defaultString(intent.getAction());
            if (action.equals(BuildConfig.INTENT_NEW_EVENT_ACTION)) {
                if (!isAdded()) {
                    return;
                }
                String data = intent.getStringExtra(getString(R.string.notification_extra));
                if (data != null && !data.trim().isEmpty()) {
                    try {
                        final Post reply = App.getInstance().getJsonMapper().readValue(data, Post.class);
                        getActivity().runOnUiThread(() -> {
                            if (adapter.getItemCount() > 0) {
                                if (adapter.getItem(0).getMid() == reply.getMid()) {
                                    adapter.addData(reply);
                                    linearLayoutManager.smoothScrollToPosition(model.list,
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
        LocalBroadcastManager.getInstance(App.getInstance()).registerReceiver(broadcastReceiver, new IntentFilter(BuildConfig.INTENT_NEW_EVENT_ACTION));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == ViewUtil.REQUEST_CODE_READ_EXTERNAL_STORAGE) {
                model.buttonAttachment.performClick();
            }
        }
    }

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();;
    }
}
