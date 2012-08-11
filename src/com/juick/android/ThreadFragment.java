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
import android.support.v4.app.SupportActivity;
import android.view.MotionEvent;
import com.juick.android.api.JuickMessage;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ListFragment;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.AdapterView;
import com.juick.R;
import com.juick.android.api.JuickUser;

/**
 *
 * @author Ugnich Anton
 */
public class ThreadFragment extends ListFragment implements AdapterView.OnItemClickListener, View.OnTouchListener, WsClientListener {

    private ThreadFragmentListener parentActivity;
    private JuickMessagesAdapter listAdapter;
    private ScaleGestureDetector mScaleDetector;
    private WsClient ws = null;
    private int mid = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScaleDetector = new ScaleGestureDetector(getActivity(), new ScaleListener());
    }

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

        getListView().setOnTouchListener(this);

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
        listAdapter = new JuickMessagesAdapter(getActivity(), JuickMessagesAdapter.TYPE_THREAD);

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

    public boolean onTouch(View view, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        return false;
    }

    public interface ThreadFragmentListener {

        public void onThreadLoaded(int uid, String nick);

        public void onReplySelected(int rid, String txt);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            listAdapter.setScale(detector.getScaleFactor());
            listAdapter.notifyDataSetChanged();
            return true;
        }
    }
}
