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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.juick.R;
import com.juick.android.api.JuickPlace;
import org.json.JSONArray;

/**
 *
 * @author Ugnich Anton
 */
public class PlacesActivity extends ListActivity implements OnItemClickListener, OnCancelListener, LocationListener {

    private static final int ACTIVITY_SETTINGS = 1;
    private static final int ACTIVITY_NEWPLACE = 2;
    private static final int ACTIVITY_PICKLOCATION = 3;
    private static final int MENUITEM_SPECIFYLOCATION = 1;
    private static final int MENUITEM_NEWPLACE = 2;
    JuickPlacesAdapter listAdapter;
    LocationManager lm;
    ProgressDialog progressDialog;
    private Location location = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setContentView(android.R.layout.pickplace);

        listAdapter = new JuickPlacesAdapter(this);
        getListView().setAdapter(listAdapter);
        getListView().setOnItemClickListener(this);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        String bestProvider = lm.getBestProvider(c, true);
        if (bestProvider == null || bestProvider.length() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setMessage(R.string.Location_determination_is_disabled);
            builder.setPositiveButton(R.string.Settings, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface arg0, int arg1) {
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), ACTIVITY_SETTINGS);
                }
            });
            builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    PlacesActivity.this.setResult(RESULT_CANCELED);
                    PlacesActivity.this.finish();
                }
            });
            builder.create().show();
            return;
        }
        lm.requestLocationUpdates(bestProvider, 0, 0, this);

        progressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.Determining_your_location___), true, true, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_NEWPLACE && resultCode == RESULT_OK && location != null) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, data);
            }
            finish();
        } else if (requestCode == ACTIVITY_PICKLOCATION && resultCode == RESULT_OK) {
            progressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.Please_wait___), true, true, this);
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            loc.setLatitude(data.getDoubleExtra("lat", 0));
            loc.setLongitude(data.getDoubleExtra("lon", 0));
            loc.setAccuracy(3000);
            onLocationChanged(loc);
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent i = new Intent(this, MessagesActivity.class);
        i.putExtra("place_id", listAdapter.getItem(position).pid);
        startActivity(i);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(MENUITEM_SPECIFYLOCATION) == null) {
            menu.add(Menu.NONE, MENUITEM_SPECIFYLOCATION, Menu.NONE, R.string.Specify_a_location).setIcon(android.R.drawable.ic_menu_mylocation);
        }
        if (menu.findItem(MENUITEM_NEWPLACE) == null) {
            menu.add(Menu.NONE, MENUITEM_NEWPLACE, Menu.NONE, R.string.New_place).setIcon(android.R.drawable.ic_menu_add);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENUITEM_SPECIFYLOCATION) {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setClass(this, PickLocationActivity.class);
            if (location != null) {
                i.putExtra("lat", location.getLatitude());
                i.putExtra("lon", location.getLongitude());
            }
            startActivityForResult(i, ACTIVITY_PICKLOCATION);
            return true;
        } else if (item.getItemId() == MENUITEM_NEWPLACE) {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setClass(this, PlaceEditActivity.class);
            i.putExtra("lat", location.getLatitude());
            i.putExtra("lon", location.getLongitude());
            startActivityForResult(i, ACTIVITY_NEWPLACE);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void onCancel(DialogInterface arg0) {
        lm.removeUpdates(this);
    }

    public void onProviderDisabled(String arg0) {
    }

    public void onProviderEnabled(String arg0) {
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
    }

    public void onLocationChanged(Location loc) {
        location = loc;
        lm.removeUpdates(this);
        listAdapter.clear();
        Thread thr = new Thread(new Runnable() {

            public void run() {
                String url = "http://api.juick.com/places?lat=" + String.valueOf(location.getLatitude()) + "&lon=" + String.valueOf(location.getLongitude());
                if (location.hasAccuracy()) {
                    url += "&acc=" + String.valueOf(location.getAccuracy());
                }
                final String jsonStr = Utils.getJSON(PlacesActivity.this, url);
                PlacesActivity.this.runOnUiThread(new Runnable() {

                    public void run() {
                        if (jsonStr != null) {
                            listAdapter.parseJSON(jsonStr);
                        }
                        progressDialog.dismiss();
                    }
                });

            }
        });
        thr.start();
    }

    class JuickPlacesAdapter extends ArrayAdapter<JuickPlace> {

        private static final int textViewResourceId = android.R.layout.simple_list_item_1;

        public JuickPlacesAdapter(Context context) {
            super(context, textViewResourceId);
        }

        public boolean parseJSON(String jsonStr) {
            try {
                JSONArray json = new JSONArray(jsonStr);
                int cnt = json.length();
                for (int i = 0; i < cnt; i++) {
                    add(JuickPlace.parseJSON(json.getJSONObject(i)));
                }
                return true;
            } catch (Exception e) {
                Log.e("initPlacesAdapter", e.toString());
            }
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView t;
            if (convertView != null && convertView instanceof TextView) {
                t = (TextView) convertView;
            } else {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                t = (TextView) vi.inflate(textViewResourceId, null);
            }
            t.setText(getItem(position).name);
            return t;
        }
    }
}

