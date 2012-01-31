/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import android.app.Activity;
import android.support.v4.app.SupportActivity;
import com.juick.android.api.JuickMessage;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import com.juick.R;
import com.juick.android.api.JuickUser;

/**
 *
 * @author ugnich
 */
public class ThreadFragment extends ListFragment implements AdapterView.OnItemClickListener, WsClientListener {

    private ThreadFragmentListener parentActivity;
    private JuickMessagesAdapter listAdapter;
    private WsClient ws = null;
    private View viewLoading;
    private int mid = 0;

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        try {
            parentActivity = (ThreadFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ThreadFragmentListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mid = args.getInt("mid", 0);
        }
        if (mid == 0) {
            return;
        }

        initWebSocket();
        initAdapter();
    }

    private void initWebSocket() {
        if (ws == null) {
            ws = new WsClient();
            ws.setListener(this);
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
        setListAdapter(new HeaderViewListAdapter(null, null, null));

        viewLoading = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_loading, null);
        getListView().addFooterView(viewLoading, null, false);

        listAdapter = new JuickMessagesAdapter(getActivity(), JuickMessagesAdapter.TYPE_THREAD);
        setListAdapter(listAdapter);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(new JuickMessageMenu(getActivity()));

        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(getActivity(), "http://api.juick.com/thread?mid=" + mid);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr != null) {
                                listAdapter.parseJSON(jsonStr);
                                if (listAdapter.getCount() > 0) {
                                    initAdapterStageTwo();
                                }
                            }
                            ThreadFragment.this.getListView().removeFooterView(viewLoading);
                        }
                    });
                }
            }
        });
        thr.start();
    }

    private void initAdapterStageTwo() {
        String replies = getResources().getString(R.string.Replies) + " (" + Integer.toString(listAdapter.getCount() - 1) + ")";
        listAdapter.addDisabledItem(replies, 1);

        final JuickUser author = listAdapter.getItem(0).User;
        parentActivity.onThreadLoaded(author.UID, author.UName);
    }

    @Override
    public void onPause() {
        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
        super.onPause();
    }

    public void onWebSocketTextFrame(final String jsonStr) {
        ((Vibrator) getActivity().getSystemService(Activity.VIBRATOR_SERVICE)).vibrate(250);
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {

                public void run() {
                    if (jsonStr != null) {
                        listAdapter.parseJSON("[" + jsonStr + "]");
                        listAdapter.getItem(1).Text = getResources().getString(R.string.Replies) + " (" + Integer.toString(listAdapter.getCount() - 2) + ")";
                    }
                }
            });
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JuickMessage jmsg = (JuickMessage) parent.getItemAtPosition(position);
        parentActivity.onReplySelected(jmsg.RID, jmsg.Text);
    }

    public interface ThreadFragmentListener {

        public void onThreadLoaded(int uid, String nick);

        public void onReplySelected(int rid, String txt);
    }
}
