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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import com.juick.android.api.JuickMessage;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import com.juick.R;
import com.juick.android.api.JuickUser;
import com.neovisionaries.ws.client.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private ThreadFragmentListener parentActivity;
    private JuickMessagesAdapter listAdapter;
    private WebSocket ws = null;
    private int mid = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
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
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        ws = Utils.getWSFactory().createSocket(new URI("wss", "ws.juick.com", "/" + mid, null));
                        ws.addHeader("Origin", "ws.juick.com");
                        ws.addHeader("Host", "ws.juick.com"); //TODO: remove from server side
                        ws.addListener(new WebSocketAdapter() {
                            @Override
                            public void onTextMessage(WebSocket websocket, final String jsonStr) throws Exception {
                                super.onTextMessage(websocket, jsonStr);
                                if (!isAdded()) {
                                    return;
                                }
                                ((Vibrator) getActivity().getSystemService(Activity.VIBRATOR_SERVICE)).vibrate(250);
                                getActivity().runOnUiThread(new Runnable() {

                                    public void run() {
                                        if (jsonStr != null) {
                                            listAdapter.parseJSON("[" + jsonStr + "]");
                                            listAdapter.getItem(1).Text = getResources().getString(R.string.Replies) + " (" + Integer.toString(listAdapter.getCount() - 2) + ")";
                                        }
                                    }
                                });
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    ws.connectAsynchronously();
                }
            });

        }
    }

    private void initAdapter() {
        listAdapter = new JuickMessagesAdapter(getActivity(), JuickMessagesAdapter.TYPE_THREAD);

        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(new JuickMessageMenu(getActivity()));

        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(getActivity(), "https://api.juick.com/thread?mid=" + mid);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr != null) {
                                listAdapter.parseJSON(jsonStr);
                                setListAdapter(listAdapter);
                                if (listAdapter.getCount() > 0) {
                                    initAdapterStageTwo();
                                }
                            }
                        }
                    });
                }
            }
        });
        thr.start();
    }

    private void initAdapterStageTwo() {
        if (!isAdded()) {
            return;
        }
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

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JuickMessage jmsg = (JuickMessage) parent.getItemAtPosition(position);
        parentActivity.onReplySelected(jmsg.RID, jmsg.Text);
    }

    public interface ThreadFragmentListener {

        public void onThreadLoaded(int uid, String nick);

        public void onReplySelected(int rid, String txt);
    }
}
