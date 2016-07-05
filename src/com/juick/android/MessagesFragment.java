/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 * Copyright (C) 2011 Johan Nilsson <https://github.com/johannilsson/android-pulltorefresh>
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

import android.widget.AbsListView;
import com.juick.android.api.JuickMessage;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.juick.R;
import java.net.URLEncoder;

/**
 *
 * @author Ugnich Anton
 */
public class MessagesFragment extends ListFragment implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener, View.OnTouchListener, View.OnClickListener {

    private JuickMessagesAdapter listAdapter;
    private JuickMessageMenu longClickListener;
    private View viewLoading;
    private String apiurl;
    private boolean loading;
    private boolean usecache;
    // Pull to refresh
    private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;
    private RelativeLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private int mCurrentScrollState;
    private int mRefreshState;
    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;
    private int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;
    private int mLastMotionY;
    private boolean mBounceHack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean home = false;
        int uid = 0;
        String uname = null;
        String search = null;
        String tag = null;
        int place_id = 0;
        boolean popular = false;
        boolean media = false;

        Bundle args = getArguments();
        if (args != null) {
            home = args.getBoolean("home", false);
            uid = args.getInt("uid", 0);
            uname = args.getString("uname");
            search = args.getString("search");
            tag = args.getString("tag");
            place_id = args.getInt("place_id", 0);
            popular = args.getBoolean("popular", false);
            media = args.getBoolean("media", false);
            usecache = args.getBoolean("usecache", false);
        }

        if (home) {
            apiurl = "https://api.juick.com/home?1=1";
        } else {
            apiurl = "https://api.juick.com/messages?1=1";
            if (uname != null) {
                apiurl += "&uname=" + uname;
            } else if (search != null) {
                try {
                    apiurl += "&search=" + URLEncoder.encode(search, "utf-8");
                } catch (Exception e) {
                    Log.e("ApiURL", e.toString());
                }
            } else if (tag != null) {
                try {
                    apiurl += "&tag=" + URLEncoder.encode(tag, "utf-8");
                } catch (Exception e) {
                    Log.e("ApiURL", e.toString());
                }
                if (uname != null) {
                    apiurl += "&uname=" + uname;
                }
                if (uid == -1) {
                    apiurl += "&uid=" + uid;
                }
            } else if (place_id > 0) {
                apiurl += "&place_id=" + place_id;
            } else if (popular) {
                apiurl += "&popular=1";
            } else if (media) {
                apiurl += "&media=all";
            }
        }

        mFlipAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        viewLoading = li.inflate(R.layout.listitem_loading, null);

        mRefreshView = (RelativeLayout) li.inflate(R.layout.pull_to_refresh_header, null);
        mRefreshViewText = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage = (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewProgress = (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);
        mRefreshViewImage.setMinimumHeight(50);
        mRefreshView.setOnClickListener(this);
        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();
        mRefreshState = TAP_TO_REFRESH;

        listAdapter = new JuickMessagesAdapter(getActivity(), 0);
        longClickListener = new JuickMessageMenu(getActivity());

        loadData();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnTouchListener(this);
        getListView().setOnScrollListener(this);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(longClickListener);

        if (usecache) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String jcacheStr = sp.getString("jcache_feed", null);
            if (jcacheStr != null) {
                processData(jcacheStr);
            }
        }
    }

    private void loadData() {
        loading = true;
        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(getActivity(), apiurl);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            processData(jsonStr);
                        }
                    });
                }
            }
        });
        thr.start();
    }

    private void processData(final String jsonStr) {
        boolean newadapter = mRefreshState != TAP_TO_REFRESH;
        if (jsonStr != null) {
            listAdapter.clear();
            int cnt = listAdapter.parseJSON(jsonStr);

            if (getListAdapter() == null) {
                getListView().addHeaderView(mRefreshView, null, false);
                mRefreshViewHeight = mRefreshView.getMeasuredHeight();

                if (cnt == 20) {
                    getListView().addFooterView(viewLoading, null, false);
                }

                setListAdapter(listAdapter);
                newadapter = true;
            } else {
                if (cnt < 20 && MessagesFragment.this.getListView().getFooterViewsCount() > 0) {
                    MessagesFragment.this.getListView().removeFooterView(viewLoading);
                }
            }

            if (usecache) {
                SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                spe.putString("jcache_feed", jsonStr);
                spe.commit();
            }
        }
        loading = false;

        resetHeader();
        getListView().invalidateViews();
        if (newadapter) {
            setSelection(1);
        }
    }

    private void loadMore(final int before_mid) {
        loading = true;
        Thread thr = new Thread(new Runnable() {

            public void run() {
                final String jsonStr = Utils.getJSON(getActivity(), apiurl + "&before_mid=" + before_mid);
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr == null || listAdapter.parseJSON(jsonStr) != 20) {
                                if (MessagesFragment.this.getListView().getFooterViewsCount() > 0) {
                                    MessagesFragment.this.getListView().removeFooterView(viewLoading);
                                }
                            }
                            loading = false;
                        }
                    });
                }
            }
        });
        thr.start();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JuickMessage jmsg = (JuickMessage) parent.getItemAtPosition(position);
        Intent i = new Intent(getActivity(), ThreadActivity.class);
        i.putExtra("mid", jmsg.MID);
        startActivity(i);
    }

    // Refresh
    public void onClick(View view) {
        mRefreshState = REFRESHING;
        prepareForRefresh();
        loadData();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (visibleItemCount < totalItemCount && (firstVisibleItem + visibleItemCount == totalItemCount) && loading == false) {
            int before_mid = listAdapter.getItem(listAdapter.getCount() - 1).MID;
            loadMore(before_mid);
        }

        // When the refresh view is completely visible, change the text to say
        // "Release to refresh..." and flip the arrow drawable.
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
                && mRefreshState != REFRESHING) {
            if (firstVisibleItem == 0) {
                mRefreshViewImage.setVisibility(View.VISIBLE);
                if ((mRefreshView.getBottom() >= mRefreshViewHeight + 20
                        || mRefreshView.getTop() >= 0)
                        && mRefreshState != RELEASE_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mFlipAnimation);
                    mRefreshState = RELEASE_TO_REFRESH;
                } else if (mRefreshView.getBottom() < mRefreshViewHeight + 20
                        && mRefreshState != PULL_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
                    if (mRefreshState != TAP_TO_REFRESH) {
                        mRefreshViewImage.clearAnimation();
                        mRefreshViewImage.startAnimation(mReverseFlipAnimation);
                    }
                    mRefreshState = PULL_TO_REFRESH;
                }
            } else {
                mRefreshViewImage.setVisibility(View.GONE);
                resetHeader();
            }
        } else if (mCurrentScrollState == SCROLL_STATE_FLING
                && firstVisibleItem == 0
                && mRefreshState != REFRESHING) {
            setSelection(1);
            mBounceHack = true;
        } else if (mBounceHack && mCurrentScrollState == SCROLL_STATE_FLING) {
            setSelection(1);
        }
    }

    public boolean onTouch(View view, MotionEvent event) {
        final int y = (int) event.getY();
        mBounceHack = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (!getListView().isVerticalScrollBarEnabled()) {
                    getListView().setVerticalScrollBarEnabled(true);
                }
                if (getListView().getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
                    if ((mRefreshView.getBottom() >= mRefreshViewHeight
                            || mRefreshView.getTop() >= 0)
                            && mRefreshState == RELEASE_TO_REFRESH) {
                        // Initiate the refresh
                        onClick(getListView());
                    } else if (mRefreshView.getBottom() < mRefreshViewHeight
                            || mRefreshView.getTop() <= 0) {
                        // Abort refresh and scroll down below the refresh view
                        resetHeader();
                        setSelection(1);
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                applyHeaderPadding(event);
                break;
        }
        return false;
    }

    private void applyHeaderPadding(MotionEvent ev) {
        // getHistorySize has been available since API 1
        int pointerCount = ev.getHistorySize();

        for (int p = 0; p < pointerCount; p++) {
            if (mRefreshState == RELEASE_TO_REFRESH) {
                if (getListView().isVerticalFadingEdgeEnabled()) {
                    getListView().setVerticalScrollBarEnabled(false);
                }

                int historicalY = (int) ev.getHistoricalY(p);

                // Calculate the padding to apply, we divide by 1.7 to
                // simulate a more resistant effect during pull.
                int topPadding = (int) (((historicalY - mLastMotionY)
                        - mRefreshViewHeight) / 1.7);

                mRefreshView.setPadding(
                        mRefreshView.getPaddingLeft(),
                        topPadding,
                        mRefreshView.getPaddingRight(),
                        mRefreshView.getPaddingBottom());
            }
        }
    }

    /**
     * Sets the header padding back to original size.
     */
    private void resetHeaderPadding() {
        mRefreshView.setPadding(
                mRefreshView.getPaddingLeft(),
                mRefreshOriginalTopPadding,
                mRefreshView.getPaddingRight(),
                mRefreshView.getPaddingBottom());
    }

    /**
     * Resets the header to the original state.
     */
    private void resetHeader() {
        if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshState = TAP_TO_REFRESH;

            resetHeaderPadding();

            // Set refresh view text to the pull label
            mRefreshViewText.setText(R.string.pull_to_refresh_tap_label);
            // Replace refresh drawable with arrow drawable
            mRefreshViewImage.setImageResource(R.drawable.ic_pulltorefresh_arrow);
            // Clear the full rotation animation
            mRefreshViewImage.clearAnimation();
            // Hide progress bar and arrow.
            mRefreshViewImage.setVisibility(View.GONE);
            mRefreshViewProgress.setVisibility(View.GONE);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mCurrentScrollState = scrollState;

        if (mCurrentScrollState == SCROLL_STATE_IDLE) {
            mBounceHack = false;
        }
    }

    public void prepareForRefresh() {
        resetHeaderPadding();

        mRefreshViewImage.setVisibility(View.GONE);
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);

        // Set refresh view text to the refreshing label
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);

        mRefreshState = REFRESHING;
    }
}
