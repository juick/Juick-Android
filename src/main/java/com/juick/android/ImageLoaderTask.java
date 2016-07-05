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

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import java.lang.ref.WeakReference;

/**
 *
 * @author ugnich
 */
public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

    private ImageCache cache;
    private final WeakReference<ImageView> imageViewReference;
    private boolean usenetwork;

    public ImageLoaderTask(ImageCache cache, ImageView imageView, boolean usenetwork) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        this.cache = cache;
        this.usenetwork = usenetwork;
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        Bitmap b = cache.getImageDisk(params[0]);
        if (b == null && usenetwork && cache.getImageNetwork(params[0], params[1])) {
            b = cache.getImageDisk(params[0]);
        }
        return b;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }
}