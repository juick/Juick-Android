/*
 * Juick
 * Copyright (C) 2008-2013, Ugnich Anton
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;
import com.jakewharton.DiskLruCache;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author Ugnich Anton
 */
public class ImageCache {

    public static final int IO_BUFFER_SIZE = 8 * 1024;
    private DiskLruCache mDiskCache;
    private LruCache<String, Bitmap> mMemoryCache;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    public ImageCache(Context context, String uniqueName, int diskCacheSize) {
        try {
            final String cachePath =
                    Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !isExternalStorageRemovable()
                    ? getExternalCacheDir(context).getPath()
                    : context.getCacheDir().getPath();
            final File diskCacheDir = new File(cachePath + File.separator + uniqueName);
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);

            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            final int cacheSize = maxMemory / 8;

            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                }
            };

        } catch (IOException e) {
            Log.e("ImageCache.ImageCache", e.toString());
        }
    }

    public Bitmap getImageMemory(String key) {
        return mMemoryCache.get(key);
    }

    public Bitmap getImageDisk(String key) {
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
            if (snapshot != null) {
                final InputStream in = snapshot.getInputStream(0);
                if (in != null) {
                    final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                    bitmap = BitmapFactory.decodeStream(buffIn);
                    if (bitmap != null) {
                        mMemoryCache.put(key, bitmap);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("ImageCache.get", e.toString());
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return bitmap;
    }

    public boolean getImageNetwork(String key, String url) {
        boolean ret = false;
        DiskLruCache.Editor editor = null;
        BufferedInputStream in = null;
        OutputStream out = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor == null) {
                return false;
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput(true);
            conn.connect();
            in = new BufferedInputStream(conn.getInputStream(), IO_BUFFER_SIZE);

            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            mDiskCache.flush();
            editor.commit();
            ret = true;
        } catch (IOException e) {
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ignored) {
            }
        }
        return ret;
    }

    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    public static File getExternalCacheDir(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }
}