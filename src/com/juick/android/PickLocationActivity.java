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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.juick.R;

/**
 *
 * @author Ugnich Anton
 */
public class PickLocationActivity extends MapActivity implements OnClickListener {

    private MapView mapView;
    private MyLocationOverlay myLocation;
    private Button bOK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map);

        bOK = (Button) findViewById(R.id.buttonOK);
        bOK.setOnClickListener(this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        myLocation = new MyLocationOverlay(this, mapView);
        myLocation.runOnFirstFix(new Runnable() {

            public void run() {
                mapView.getController().setCenter(myLocation.getMyLocation());
                mapView.getController().setZoom(17);
            }
        });
        myLocation.enableMyLocation();
        mapView.getOverlays().add(myLocation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        myLocation.disableMyLocation();
    }

    public void onClick(View v) {
        Intent i = new Intent();
        GeoPoint center = mapView.getMapCenter();
        i.putExtra("lat", ((double) center.getLatitudeE6()) / 1000000);
        i.putExtra("lon", ((double) center.getLongitudeE6()) / 1000000);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
