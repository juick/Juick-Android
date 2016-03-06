/*
 * Juick
 * Copyright (C) 2008-2013, ugnich
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.juick.R;
import com.juick.android.api.JuickMessage;
import org.json.JSONArray;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author ugnich
 */
public class PMFragment extends ListFragment {

    private PMFragmentListener parentActivity;
    private PMAdapter listAdapter = null;
    private String uname;
    private int uid;

    private String myname;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            parentActivity = (PMFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PMFragmentListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        myname = getMyName(getActivity());
        uname = getArguments().getString("uname");
        uid = getArguments().getInt("uid", 0);

        listAdapter = new PMAdapter(getActivity(), uid, uname, myname);

        Thread thr = new Thread(new Runnable() {

            public void run() {
                String url = "https://api.juick.com/pm?uname=" + uname;
                String jsonStr = Utils.getJSON(getActivity(), url);
                if (isAdded()) {
                    onNewMessages(jsonStr);
                }
            }
        });
        thr.start();
    }
    /* dirty hack */
    public static String getMyName(Context context) {
        AccountManager am = AccountManager.get(context);
        Account accs[] = am.getAccountsByType(context.getString(R.string.com_juick));
        if (accs.length > 0) {
            Bundle b = null;
            try {
                b = am.getAuthToken(accs[0], "", false, null, null).getResult();
            } catch (Exception e) {
                Log.e("getBasicAuthString", Log.getStackTraceString(e));
            }
            if (b != null) {
                return b.getString(AccountManager.KEY_ACCOUNT_NAME);
            }
        }
        return "";
    }

    public void onNewMessages(final String msg) {
        if (listAdapter != null) {
            getActivity().runOnUiThread(new Runnable() {

                public void run() {
                    try {
                        if(msg != null)
                            listAdapter.parseJSON(msg);
                        setListAdapter(listAdapter);
                        if(listAdapter.getCount() > 0)
                            getListView().setSelection(listAdapter.getCount() - 1);

                    } catch (Exception e) {
                        Log.e("PMFragment.onNewMessage", e.toString());
                    }
                }
            });
        }
    }

    public interface PMFragmentListener {
    }
}

class PMAdapter extends ArrayAdapter<JuickMessage> {

    Context context;
    int uid;
    String uname;
    String myname;
    private ImageCache userpics;

    public PMAdapter(Context context, int uid, String uname, String myname) {
        super(context, R.layout.listitem_pm_in);
        this.context = context;
        this.uid = uid;
        this.uname = uname;
        this.myname = myname;
        userpics = new ImageCache(context, "userpics-small", 1024 * 1024 * 1);
    }

    public int parseJSON(String jsonStr) {
        try {
            JSONArray json = new JSONArray(jsonStr);
            int cnt = json.length();
            for (int i = cnt-1; i >= 0; i--) {
                add(JuickMessage.parseJSON(json.getJSONObject(i)));
            }
            return cnt;
        } catch (Exception e) {
            Log.e("initOpinionsAdapter", e.toString());
        }

        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        JuickMessage msg = getItem(position);

        View v = convertView;
        TextView tv;
        if (msg.User.UID == uid) {
            if (v == null || !v.getTag().toString().equals("i")) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.listitem_juickmessage, null);
                v.setTag("i");
                v.findViewById(R.id.comment).setVisibility(View.GONE);
                v.findViewById(R.id.replies).setVisibility(View.GONE);
            }
            tv = (TextView) v.findViewById(R.id.username);
            tv.setText(uname);
        } else {
            if (v == null || !v.getTag().toString().equals("o")) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.listitem_juickmessage, null);
                v.setTag("o");
                v.findViewById(R.id.comment).setVisibility(View.GONE);
                v.findViewById(R.id.replies).setVisibility(View.GONE);
            }
            tv = (TextView) v.findViewById(R.id.username);
            tv.setText(myname);
        }
        tv = (TextView) v.findViewById(R.id.text);
        tv.setText(msg.Text);
        tv = (TextView) v.findViewById(R.id.timestamp);
        tv.setText(formatMessageTimestamp(msg.Timestamp));

        ImageView upic = (ImageView) v.findViewById(R.id.userpic);
        Bitmap bitmapupic = userpics.getImageMemory(Integer.toString(msg.User.UID));
        if (bitmapupic != null) {
            upic.setImageBitmap(bitmapupic);
        } else {
            upic.setImageResource(R.drawable.ic_user_32);
            ImageLoaderTask task = new ImageLoaderTask(userpics, upic, true);
            task.execute(Integer.toString(msg.User.UID), "https://i.juick.com/as/" + msg.User.UID + ".png");
        }

        return v;
    }
    private String formatMessageTimestamp(Date jmsg) {
        DateFormat df = new SimpleDateFormat("HH:mm dd/MMM/yy");
        df.setTimeZone(TimeZone.getDefault());
        return df.format(jmsg);
    }
}
