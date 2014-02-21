/*
 * Juick
 * Copyright (C) 2014, ugnich
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 *
 * @author ugnich
 */
public class WebViewClientJuick extends WebViewClient {

    Activity activity;
    WebViewClientListener listener = null;

    public WebViewClientJuick(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        boolean openBrowser = true;

        Uri u = Uri.parse(url);
        String file = u.getLastPathSegment();
        if (u.getHost().equals("juick.com") || u.getHost().endsWith(".juick.com")) {
            openBrowser = false;
        } else if (file != null && (file.endsWith(".jpg") || file.endsWith(".png") || file.endsWith(".gif"))) {
            openBrowser = false;
        }

        if (openBrowser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
        }
        return openBrowser;
    }

    public void setWebViewClientListener(WebViewClientListener wvcl) {
        listener = wvcl;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (listener != null) {
            listener.setProgressBarVisible(true);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (listener != null) {
            listener.setProgressBarVisible(false);
        }
    }

    public interface WebViewClientListener {

        public void setProgressBarVisible(boolean visible);
    }
}
