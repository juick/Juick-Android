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

import android.text.Layout.Alignment;
import com.juick.android.api.JuickMessage;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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

    private static final String PREFERENCES_SCALE = "MessagesListScale";
    public static final int TYPE_THREAD = 1;
    public static Pattern urlPattern = Pattern.compile("((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
    public static Pattern msgPattern = Pattern.compile("#[0-9]+");
//    public static Pattern usrPattern = Pattern.compile("@[a-zA-Z0-9\\-]{2,16}");
    private String Replies;
    private int type;
    private boolean allItemsEnabled = true;
    private SharedPreferences sp;
    private float defaultTextSize;
    private float textScale;

    public JuickMessagesAdapter(Context context, int type) {
        super(context, R.layout.listitem_juickmessage);
        Replies = context.getResources().getString(R.string.Replies_) + " ";
        this.type = type;

        defaultTextSize = new TextView(context).getTextSize();

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        textScale = sp.getFloat(PREFERENCES_SCALE, 1);
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
            }
            TextView t = (TextView) v.findViewById(R.id.text);
            t.setTextSize(defaultTextSize * textScale);

            if (type == TYPE_THREAD && jmsg.RID == 0) {
                t.setText(formatFirstMessageText(jmsg));
            } else {
                t.setText(formatMessageText(jmsg));
            }

            /*
            ImageView i = (ImageView) v.findViewById(R.id.icon);
            if (jmsg.User != null && jmsg.User.Avatar != null) {
            i.setImageDrawable((Drawable) jmsg.User.Avatar);
            } else {
            i.setImageResource(R.drawable.ic_user_32);
            }
             */

        } else {
            if (v == null || !(v instanceof TextView)) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.preference_category, null);
            }

            ((TextView) v).setTextSize(defaultTextSize * textScale);

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
        allItemsEnabled = false;
        JuickMessage jmsg = new JuickMessage();
        jmsg.Text = txt;
        insert(jmsg, position);
    }

    public void setScale(float scale) {
        textScale *= scale;
        textScale = Math.max(0.5f, Math.min(textScale, 2.0f));
        sp.edit().putFloat(PREFERENCES_SCALE, textScale).commit();
    }

    private SpannableStringBuilder formatMessageText(JuickMessage jmsg) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String name = '@' + jmsg.User.UName;
        String tags = jmsg.getTags();
        String txt = jmsg.Text;
        if (jmsg.Photo != null) {
            txt = jmsg.Photo + "\n" + txt;
        }
        if (jmsg.Video != null) {
            txt = jmsg.Video + "\n" + txt;
        }
        ssb.append(name + ' ' + tags + "\n" + txt);
        ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(0xFFC8934E), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (tags.length() > 0)
            ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), name.length() + 1, 
                    name.length() + tags.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int paddingt = name.length() + 1 + tags.length() + 1;

        // Highlight links http://example.com/
        int pos = 0;
        Matcher m = urlPattern.matcher(txt);
        while (m.find(pos)) {
            ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), paddingt + m.start(), paddingt + m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        // Highlight messages #1234
        pos = 0;
        m = msgPattern.matcher(txt);
        while (m.find(pos)) {
            ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), paddingt + m.start(), paddingt + m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        /*
        // Highlight usernames @username
        pos = 0;
        m = usrPattern.matcher(txt);
        while (m.find(pos)) {
        ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), paddingt + m.start(), paddingt + m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        pos = m.end();
        }
         */
        
        DateFormat df = new SimpleDateFormat("HH:mm dd/MMM/yy");
        df.setTimeZone(TimeZone.getDefault());
        String date = df.format(jmsg.Timestamp);
        ssb.append("\n" + date + " ");

        int padding = name.length() + 1 + tags.length() + 1 + txt.length() + 1;
        int end = padding + date.length() + 1;

        ssb.setSpan(new ForegroundColorSpan(0xFFAAAAAA), padding, padding + date.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (jmsg.replies > 0) {
            String replies = Replies + jmsg.replies;
            ssb.append("  " + replies + " ");
            end += 2 + replies.length() + 1;
            int padding2 = padding + date.length() + 1 + 2;
            ssb.setSpan(new ForegroundColorSpan(0xFFC8934E), padding2, padding2 + replies.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        ssb.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_OPPOSITE), padding, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return ssb;
    }

    private SpannableStringBuilder formatFirstMessageText(JuickMessage jmsg) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String tags = jmsg.getTags();
        if (tags.length() > 0) {
            tags += "\n";
        }
        String txt = jmsg.Text;
        if (jmsg.Photo != null) {
            txt = jmsg.Photo + "\n" + txt;
        }
        if (jmsg.Video != null) {
            txt = jmsg.Video + "\n" + txt;
        }
        ssb.append(tags + txt);
        if (tags.length() > 0) {
            ssb.setSpan(new ForegroundColorSpan(0xFF000099), 0, tags.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int paddingt = tags.length();
        int pos = 0;
        Matcher m = urlPattern.matcher(txt);
        while (m.find(pos)) {
            ssb.setSpan(new ForegroundColorSpan(0xFF0000CC), paddingt + m.start(), paddingt + m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        return ssb;
    }
}
