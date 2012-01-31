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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.juick.R;
import java.net.URLEncoder;
import org.json.JSONObject;

/**
 *
 * @author Ugnich Anton
 */
public class PlaceEditActivity extends MapActivity implements OnClickListener, TabHost.OnTabChangeListener {

    private static final String TABDETAILS = "details";
    private static final String TABMAP = "map";
    private EditText etName;
    private EditText etDescription;
    private EditText etTags;
    private EditText etURL;
    private double lat;
    private double lon;
    private MapView mapView;
    private MyLocationOverlay myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        lat = i.getDoubleExtra("lat", 0);
        lon = i.getDoubleExtra("lon", 0);
        if (lat == 0 && lon == 0) {
            setResult(RESULT_CANCELED);
            finish();
        }

        setContentView(R.layout.placeedit);

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec(TABDETAILS).setIndicator(getResources().getString(R.string.Details), getResources().getDrawable(R.drawable.ic_tab_info)).setContent(R.id.tabDetails));
        tabHost.addTab(tabHost.newTabSpec(TABMAP).setIndicator(getResources().getString(R.string.Map), getResources().getDrawable(R.drawable.ic_tab_map)).setContent(R.id.tabMap));
        tabHost.setOnTabChangedListener(this);

        etName = (EditText) findViewById(R.id.editName);
        etDescription = (EditText) findViewById(R.id.editDescription);
        etTags = (EditText) findViewById(R.id.editTags);
        etURL = (EditText) findViewById(R.id.editURL);
        ((Button) findViewById(R.id.buttonAdd)).setOnClickListener(this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setCenter(new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)));
        mapView.getController().setZoom(18);
        myLocation = new MyLocationOverlay(this, mapView);
        mapView.getOverlays().add(myLocation);
    }

    public void onTabChanged(String tabId) {
        if (tabId.equals(TABMAP)) {
            myLocation.enableMyLocation();
        } else {
            myLocation.disableMyLocation();
        }
    }

    public void onClick(View v) {
        final String name = etName.getText().toString();
        final String descr = etDescription.getText().toString();
        final String tags = etTags.getText().toString();
        final String url = etURL.getText().toString();
        if (name.length() == 0) {
            Toast.makeText(this, R.string.Enter_a_name, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.Please_wait___, Toast.LENGTH_SHORT).show();

        GeoPoint center = mapView.getMapCenter();
        lat = ((double) center.getLatitudeE6()) / 1000000;
        lon = ((double) center.getLongitudeE6()) / 1000000;

        try {
            String data = "lat=" + lat + "&lon=" + lon + "&name=" + URLEncoder.encode(name, "utf-8");
            if (descr.length() > 0) {
                data += "&description=" + URLEncoder.encode(descr, "utf-8");
            }
            if (tags.length() > 0) {
                data += "&tags=" + URLEncoder.encode(tags, "utf-8");
            }
            if (url.length() > 0 && !url.equals("http://")) {
                data += "&url=" + URLEncoder.encode(url, "utf-8");
            }
            final String dataf = data;
            Thread thr = new Thread(new Runnable() {

                public void run() {
                    final String jsonStr = Utils.postJSON(PlaceEditActivity.this, "http://api.juick.com/place_add", dataf);
                    PlaceEditActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            if (jsonStr != null) {
                                try {
                                    JSONObject json = new JSONObject(jsonStr);
                                    if (json.has("pid")) {
                                        Intent i = new Intent();
                                        i.putExtra("lat", lat);
                                        i.putExtra("lon", lon);
                                        i.putExtra("pid", json.getInt("pid"));
                                        i.putExtra("pname", name);
                                        setResult(RESULT_OK, i);
                                        finish();
                                        return;
                                    }
                                } catch (Exception e) {
                                    Log.e("PlaceEditParseJSON", e.toString());
                                }
                            }
                            Toast.makeText(PlaceEditActivity.this, R.string.Error, Toast.LENGTH_LONG).show();
                        }
                    });

                }
            });
            thr.start();
        } catch (Exception e) {
            Log.e("PlaceEditSubmit", e.toString());
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
