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

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.juick.R;

/**
 *
 * @author Ugnich Anton
 */
public class AttachAdapter extends BaseAdapter {

    private Context context;
    private final int icons[] = {
        R.drawable.ic_attach_photo,
        R.drawable.ic_attach_photo_new,
        R.drawable.ic_attach_video,
        R.drawable.ic_attach_video_new
    };
    private final int labels[] = {
        R.string.Photo_from_gallery,
        R.string.New_photo,
        R.string.Video_from_gallery,
        R.string.New_video
    };

    public AttachAdapter(Context context) {
        super();
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            tv = (TextView) vi.inflate(android.R.layout.simple_list_item_1, null);
            tv.setCompoundDrawablePadding(10);
            tv.setTextColor(Color.BLACK);
        }
        if (position >= 0 && position < 4) {
            tv.setText(labels[position]);
            tv.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0);
        }
        return tv;
    }

    public int getCount() {
        return labels.length;
    }

    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        if (position >= 0 && position < 4) {
            return context.getResources().getString(labels[position]);
        } else {
            return "";
        }
    }
}
