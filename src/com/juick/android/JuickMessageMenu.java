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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;
import com.juick.R;
import com.juick.android.api.JuickMessage;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessageMenu implements OnItemLongClickListener, OnClickListener {

    Activity activity;
    JuickMessage listSelectedItem;
    ArrayList<String> urls;
    int menuLength;

    public JuickMessageMenu(Activity activity) {
        this.activity = activity;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        listSelectedItem = (JuickMessage) parent.getAdapter().getItem(position);

        urls = new ArrayList<String>();
        if (listSelectedItem.Photo != null) {
            urls.add(listSelectedItem.Photo);
        }
        if (listSelectedItem.Video != null) {
            urls.add(listSelectedItem.Video);
        }

        int pos = 0;
        Matcher m = JuickMessagesAdapter.urlPattern.matcher(listSelectedItem.Text);
        while (m.find(pos)) {
            urls.add(listSelectedItem.Text.substring(m.start(), m.end()));
            pos = m.end();
        }

        pos = 0;
        m = JuickMessagesAdapter.msgPattern.matcher(listSelectedItem.Text);
        while (m.find(pos)) {
            urls.add(listSelectedItem.Text.substring(m.start(), m.end()));
            pos = m.end();
        }
        /*
        pos = 0;
        m = JuickMessagesAdapter.usrPattern.matcher(listSelectedItem.Text);
        while (m.find(pos)) {
        urls.add(listSelectedItem.Text.substring(m.start(), m.end()));
        pos = m.end();
        }
         */
        menuLength = 4 + urls.size();
        if (listSelectedItem.RID == 0) {
            menuLength++;
        }
        CharSequence[] items = new CharSequence[menuLength];
        int i = 0;
        if (urls.size() > 0) {
            for (String url : urls) {
                items[i++] = url;
            }
        }
        if (listSelectedItem.RID == 0) {
            items[i++] = activity.getResources().getString(R.string.Recommend_message);
        }
        String UName = listSelectedItem.User.UName;
        items[i++] = '@' + UName + " " + activity.getResources().getString(R.string.blog);
        items[i++] = activity.getResources().getString(R.string.Subscribe_to) + " @" + UName;
        items[i++] = activity.getResources().getString(R.string.Blacklist) + " @" + UName;
        items[i++] = activity.getResources().getString(R.string.Share);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setItems(items, this);
        builder.create().show();
        return true;
    }

    public void onClick(DialogInterface dialog, int which) {
        if (urls != null) {
            if (which < urls.size()) {
                String url = urls.get(which);
                if (url.startsWith("#")) {
                    int mid = Integer.parseInt(url.substring(1));
                    if (mid > 0) {
                        Intent intent = new Intent(activity, ThreadActivity.class);
                        intent.putExtra("mid", mid);
                        activity.startActivity(intent);
                    }
                    //} else if (url.startsWith("@")) {
                } else {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
                return;
            }
            which -= urls.size();
        }
        if (listSelectedItem.RID != 0) {
            which += 1;
        }
        switch (which) {
            case 0:
                postMessage("! #" + listSelectedItem.MID, activity.getResources().getString(R.string.Recommended));
                break;
            case 1:
                Intent i = new Intent(activity, MessagesActivity.class);
                i.putExtra("uid", listSelectedItem.User.UID);
                i.putExtra("uname", listSelectedItem.User.UName);
                activity.startActivity(i);
                break;
            case 2:
                postMessage("S @" + listSelectedItem.User.UName, activity.getResources().getString(R.string.Subscribed));
                break;
            case 3:
                postMessage("BL @" + listSelectedItem.User.UName, activity.getResources().getString(R.string.Added_to_BL));
                break;
            case 4:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, listSelectedItem.toString());
                activity.startActivity(intent);
                break;
        }
    }

    private void postMessage(final String body, final String ok) {
        Thread thr = new Thread(new Runnable() {

            public void run() {
                try {
                    final String ret = Utils.postJSON(activity, "http://api.juick.com/post", "body=" + URLEncoder.encode(body, "utf-8"));
                    activity.runOnUiThread(new Runnable() {

                        public void run() {
                            Toast.makeText(activity, (ret != null) ? ok : activity.getResources().getString(R.string.Error), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("postMessage", e.toString());
                }
            }
        });
        thr.start();
    }
}
