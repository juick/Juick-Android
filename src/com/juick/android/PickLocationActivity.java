/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * @author ugnich
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
        mapView.getOverlays().add(myLocation);

        Intent i = getIntent();
        double lat = i.getDoubleExtra("lat", 0);
        double lon = i.getDoubleExtra("lon", 0);
        if (lat != 0 && lon != 0) {
            mapView.getController().setCenter(new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)));
            mapView.getController().setZoom(18);
        }
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
