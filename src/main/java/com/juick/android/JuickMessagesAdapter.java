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

import com.juick.android.api.JuickMessage;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.juick.R;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessagesAdapter extends ArrayAdapter<JuickMessage> {

    private static final String PREFERENCES_FONTSIZE = "fontsizesp";
    public static final int TYPE_THREAD = 1;
    public static Pattern urlPattern = Pattern.compile("((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
    public static Pattern msgPattern = Pattern.compile("#[0-9]+");
//    public static Pattern usrPattern = Pattern.compile("@[a-zA-Z0-9\\-]{2,16}");
    private ImageCache userpics;
    private ImageCache photos;
    private boolean usenetwork = false;
    private int type;
    private SharedPreferences sp;
    private float textSize;

    public JuickMessagesAdapter(Context context, int type) {
        super(context, R.layout.listitem_juickmessage);
        this.type = type;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String textScaleStr = sp.getString(PREFERENCES_FONTSIZE, "16");
        try {
            textSize = Float.parseFloat(textScaleStr);
        } catch (Exception e) {
            textSize = 16;
        }

        String loadphotos = sp.getString("loadphotos", "Always");
        if (loadphotos.charAt(0) == 'A' || (loadphotos.charAt(0) == 'W' && Utils.isWiFiConnected(context))) {
            usenetwork = true;
        }

        photos = new ImageCache(context, "photos-small", 1024 * 1024 * 5);
        userpics = new ImageCache(context, "userpics-small", 1024 * 1024 * 2);
    }

    public int parseJSON(String jsonStr) {
        try {
            JSONArray json = new JSONArray(jsonStr);
            int cnt = json.length();
            for (int i = 0; i < cnt; i++) {
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
        JuickMessage jmsg = getItem(position);
        View v = convertView;

        if (jmsg.User != null && jmsg.Text != null) {
            if (v == null || !(v instanceof LinearLayout)) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.listitem_juickmessage, null);
                ((TextView) v.findViewById(R.id.text)).setTextSize(textSize);
                if (type == TYPE_THREAD) {
                    v.findViewById(R.id.comment).setVisibility(View.GONE);
                    v.findViewById(R.id.replies).setVisibility(View.GONE);
                }
            }

            ImageView upic = (ImageView) v.findViewById(R.id.userpic);
            Bitmap bitmapupic = photos.getImageMemory(Integer.toString(jmsg.User.UID));
            if (bitmapupic != null) {
                upic.setImageBitmap(bitmapupic);
            } else {
                upic.setImageResource(R.drawable.ic_user_32);
                ImageLoaderTask task = new ImageLoaderTask(userpics, upic, usenetwork);
                task.execute(Integer.toString(jmsg.User.UID), "https://i.juick.com/as/" + jmsg.User.UID + ".png");
            }

            TextView username = (TextView) v.findViewById(R.id.username);
            username.setText(jmsg.User.UName);

            TextView timestamp = (TextView) v.findViewById(R.id.timestamp);
            timestamp.setText(formatMessageTimestamp(jmsg));

            TextView t = (TextView) v.findViewById(R.id.text);
            if (type == TYPE_THREAD && jmsg.RID == 0) {
                t.setText(formatFirstMessageText(jmsg));
            } else {
                t.setText(formatMessageText(jmsg));
            }

            ImageView p = (ImageView) v.findViewById(R.id.photo);
            if (jmsg.Photo != null) {
                String key = Integer.toString(jmsg.MID) + "-" + Integer.toString(jmsg.RID);
                Bitmap bitmap = photos.getImageMemory(key);
                if (bitmap != null) {
                    p.setImageBitmap(bitmap);
                } else {
                    p.setImageResource(R.drawable.ic_attach_photo);
                    ImageLoaderTask task = new ImageLoaderTask(photos, p, usenetwork);
                    task.execute(key, jmsg.Photo);
                }
                p.setVisibility(View.VISIBLE);
            } else {
                p.setVisibility(View.GONE);
            }

            if (jmsg.replies > 0 && type != TYPE_THREAD) {
                TextView replies = (TextView) v.findViewById(R.id.replies);
                replies.setVisibility(View.VISIBLE);
                replies.setText(Integer.toString(jmsg.replies));
            } else {
                v.findViewById(R.id.replies).setVisibility(View.GONE);
            }

        } else {
            if (v == null || !(v instanceof TextView)) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.preference_category, null);
            }

            ((TextView) v).setTextSize(textSize);

            if (jmsg.Text != null) {
                ((TextView) v).setText(jmsg.Text);
            } else {
                ((TextView) v).setText("");
            }
        }

        return v;
    }

    @Override
    public boolean isEnabled(int position) {
        JuickMessage jmsg = getItem(position);
        return (jmsg != null && jmsg.User != null && jmsg.MID > 0);
    }

    public void addDisabledItem(String txt, int position) {
        JuickMessage jmsg = new JuickMessage();
        jmsg.Text = txt;
        insert(jmsg, position);
    }

    private String formatMessageTimestamp(JuickMessage jmsg) {
        DateFormat df = new SimpleDateFormat("HH:mm dd/MMM/yy");
        df.setTimeZone(TimeZone.getDefault());
        return df.format(jmsg.Timestamp);
    }

    private SpannableStringBuilder formatMessageText(JuickMessage jmsg) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(jmsg.Text);

        // Highlight links http://example.com/
        int pos = 0;
        Matcher m = urlPattern.matcher(jmsg.Text);
        while (m.find(pos)) {
            ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        // Highlight messages #1234
        pos = 0;
        m = msgPattern.matcher(jmsg.Text);
        while (m.find(pos)) {
            ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        /*
        // Highlight usernames @username
        pos = 0;
        m = usrPattern.matcher(txt);
        while (m.find(pos)) {
        ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        pos = m.end();
        }
         */

        return ssb;
    }

    private SpannableStringBuilder formatFirstMessageText(JuickMessage jmsg) {
        SpannableStringBuilder ssb = formatMessageText(jmsg);
        String tags = jmsg.getTags();
        if (tags.length() > 0) {
            int padding = ssb.length();
            ssb.append("\n" + tags);
            ssb.setSpan(new ForegroundColorSpan(0xFF999999), padding, padding + 1 + tags.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }
}
