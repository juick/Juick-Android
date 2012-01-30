/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import com.juick.android.api.JuickMessage;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.juick.R;
import java.net.URLEncoder;

/**
 *
 * @author ugnich
 */
public class ThreadActivity extends ListActivity implements OnItemClickListener, View.OnClickListener, WsClientListener {

    private JuickMessagesAdapter listAdapter;
    private WsClient ws = null;
    private View viewLoading;
    private TextView tvReplyTo;
    private EditText etMessage;
    private Button bSend;
    private int mid = 0;
    private int rid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        mid = i.getIntExtra("mid", 0);
        if (mid == 0) {
            finish();
        }

        setContentView(R.layout.thread);
        tvReplyTo = (TextView) findViewById(R.id.textReplyTo);
        etMessage = (EditText) findViewById(R.id.editMessage);
        bSend = (Button) findViewById(R.id.buttonSend);

        bSend.setOnClickListener(this);
        initWebSocket();
        initAdapter();
    }

    private void initWebSocket() {
        if (ws == null) {
            ws = new WsClient();
            ws.setListener(ThreadActivity.this);
        }
        Thread wsthr = new Thread(new Runnable() {

            public void run() {
                if (ws.connect("api.juick.com", 8080, "/replies/" + mid, null) && ws != null) {
                    ws.readLoop();
                }
            }
        });
        wsthr.start();
    }

    private void initAdapter() {
        getListView().setAdapter(new HeaderViewListAdapter(null, null, null));

        viewLoading = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_loading, null);
        getListView().addFooterView(viewLoading, null, false);

        listAdapter = new JuickMessagesAdapter(this, JuickMessagesAdapter.TYPE_THREAD);
        getListView().setAdapter(listAdapter);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(new JuickMessageMenu(this));

        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(ThreadActivity.this, "http://api.juick.com/thread?mid=" + mid);
                ThreadActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        if (jsonStr != null) {
                            listAdapter.parseJSON(jsonStr);
                            if (listAdapter.getCount() > 0) {
                                initAdapterStageTwo();
                            }
                        }
                        ThreadActivity.this.getListView().removeFooterView(viewLoading);
                    }
                });
            }
        });
        thr.start();
    }

    private void initAdapterStageTwo() {
//        if (listAdapter.getCount() > 1) {
        String replies = getResources().getString(R.string.Replies) + " (" + Integer.toString(listAdapter.getCount() - 1) + ")";
        listAdapter.addDisabledItem(replies, 1);
//        }

        final JuickMessage jmsgfirst = listAdapter.getItem(0);

        final TextView title = (TextView) findViewById(android.R.id.title);
        title.setText("@" + jmsgfirst.User.UName);
        title.setCompoundDrawablePadding(8);
        title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_user_32, 0, 0, 0);
        Thread thr = new Thread(new Runnable() {

            public void run() {
                final Bitmap avatarb = Utils.downloadImage("http://i.juick.com/a/" + jmsgfirst.User.UID + ".png");
                if (avatarb != null) {
                    ThreadActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);
                            Bitmap avatar48 = Bitmap.createScaledBitmap(avatarb, (int) (48 * metrics.density), (int) (48 * metrics.density), false);
                            title.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(avatar48), null, null, null);
                        }
                    });
                }
            }
        });
        thr.start();
    }

    @Override
    protected void onPause() {
        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
        super.onPause();
    }

    public void onWebSocketTextFrame(final String jsonStr) {
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(250);
        ThreadActivity.this.runOnUiThread(new Runnable() {

            public void run() {
                if (jsonStr != null) {
                    listAdapter.parseJSON("[" + jsonStr + "]");
                    listAdapter.getItem(1).Text = getResources().getString(R.string.Replies) + " (" + Integer.toString(listAdapter.getCount() - 2) + ")";
                }
            }
        });
    }

    private void resetForm() {
        rid = 0;
        tvReplyTo.setVisibility(View.GONE);
        etMessage.setText("");
        etMessage.setEnabled(true);
        bSend.setEnabled(true);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JuickMessage jmsg = (JuickMessage) parent.getItemAtPosition(position);
        rid = jmsg.RID;
        if (rid > 0) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            String inreplyto = getResources().getString(R.string.In_reply_to_) + " ";
            ssb.append(inreplyto + jmsg.Text);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, inreplyto.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvReplyTo.setText(ssb);
            tvReplyTo.setVisibility(View.VISIBLE);
        } else {
            tvReplyTo.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        String msg = etMessage.getText().toString();
        if (msg.length() < 3) {
            Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }
//        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

        String msgnum = "#" + mid;
        if (rid > 0) {
            msgnum += "/" + rid;
        }
        final String body = msgnum + " " + msg;

        etMessage.setEnabled(false);
        bSend.setEnabled(false);

        Thread thr = new Thread(new Runnable() {

            public void run() {
                try {
                    final String ret = Utils.postJSON(ThreadActivity.this, "http://api.juick.com/post", "body=" + URLEncoder.encode(body, "utf-8"));
                    ThreadActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            if (ret != null) {
                                Toast.makeText(ThreadActivity.this, R.string.Message_posted, Toast.LENGTH_SHORT).show();
                                resetForm();
                            } else {
                                Toast.makeText(ThreadActivity.this, R.string.Error, Toast.LENGTH_SHORT).show();
                                etMessage.setEnabled(true);
                                bSend.setEnabled(true);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("postComment", e.toString());
                }
            }
        });
        thr.start();
    }
}
