package com.example.volkerpetersen.sailingrace;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import java.text.DecimalFormat;
import java.util.LinkedList;

/**
 * Created by Volker Petersen November 2015.
 */
//-----------------------------------------------------------------------------------------
// GPS location, heading, and speed service
//
// @param   - Context activity context
// @param   - long gpsUpdate: minimum time (in milliseconds) between GPS updates
// @param   - float gpsDistance: minimum distance (in meters) between GPS updates
// @param   - int keepNumPositions: max GPS location history to compute average speed / heading
//
//-----------------------------------------------------------------------------------------
public class GPSTracker extends Service implements LocationListener {
    private GeomagneticField geoField;
    private final Context context;
    private final GlobalParameters para;
    private boolean isGPSEnabled = false;
    private boolean canGetLocation = false;
    private int GPSfired;
    private int keepNumPositions;
    private double toKts = 1.94384;  // m/s to knots conversation factor
    private Location location;
    private LocationManager locationManager;
    public LinkedList<Location> LocationList = new LinkedList<Location>();
    private String[] dots;
    private DecimalFormat dfThree = new DecimalFormat("000");
    static final String LOG_TAG = GPSTracker.class.getSimpleName();

    public GPSTracker(Context context, GlobalParameters para, long gpsUpdates, float gpsDistance, int keepNumPositions) {
        this.context = context;
        this.para = para;
        this.keepNumPositions = keepNumPositions;
        location = getLocation(gpsUpdates, gpsDistance);
        GPSfired = 0;
        dots = new String[]{".   ", "..  ", "... ", "...."};
        if (location != null) {
            geoField = new GeomagneticField(
                    Double.valueOf(location.getLatitude()).floatValue(),
                    Double.valueOf(location.getLongitude()).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );
            para.declination = geoField.getDeclination();
        } else {
            para.declination = 0.0;
        }
    }

    public Location getLocation(long minGPSUpdateTime, float minGPSDistance) {
        // @param  minGPSUpdateTime in milliseconds
        // @param  minGPSDistance in meter
        Location loc = null;
        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (isGPSEnabled) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minGPSUpdateTime, minGPSDistance, this);
                } catch (SecurityException e) {
                    Log.e(LOG_TAG, "Location PERMISSION_NOT_GRANTED by user");
                }

                if (locationManager != null) {
                    try {
                        loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    } catch (SecurityException e) {
                        Log.e(LOG_TAG,"Location PERMISSION_NOT_GRANTED by user");
                    }

                    if (loc != null) {
                        this.canGetLocation = true;
                        para.latitude = this.getLatitude();
                        para.longitude = this.getLongitude();
                        this.getHeading();
                        this.getSpeed();
                        this.getAvgSpeedHeading();
                    }
                }
            } else {
                // TODO generate error msg
            }
            if (loc != null) {
                location = loc;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loc;
    }

    public void stopUsingGPS() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(GPSTracker.this);
            } catch (SecurityException e) {
                Log.e(LOG_TAG,"Location PERMISSION_NOT_GRANTED by user");
            }
        }
    }

    public double getLatitude() {
        if (location != null) para.latitude = location.getLatitude();
        return para.latitude;
    }

    public double getLongitude() {
        if (location != null) para.longitude = location.getLongitude();
        return para.longitude;
    }

    public void getSpeed() {
        if (location != null) para.SOG = location.getSpeed() * toKts;
    }

    public void getHeading() {
        if (location != null) {
            para.COG = location.getBearing() + para.declination;
        }
    }

    public void getAvgSpeedHeading() {
        if (location != null && LocationList.size()>2 ) {
            Location first = LocationList.getFirst();
            Location last = LocationList.getLast();

            para.avgCOG = NavigationTools.fixAngle(first.bearingTo(last) + para.declination);
            double distance = first.distanceTo(last);
            double time = (last.getElapsedRealtimeNanos() - first.getElapsedRealtimeNanos()) / 1000000000.0;
            if (time != 0.0) {
                para.avgSOG = distance / time * toKts;
            }
        } else {
            para.avgCOG = para.COG;
            para.avgSOG = para.SOG;
        }
    }

    public String getBestProvider() {
        String provider = "unknown";
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        if (locationManager != null) {
            provider = locationManager.getBestProvider(criteria, true);
        }
        return provider;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        alertDialog.setTitle("GPS warning");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to the settings menu and enable GPS?");

        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location locUpdate) {
        String provider = getBestProvider();
        location = locUpdate;
        if (LocationList.size() >= keepNumPositions) {
            LocationList.removeFirst();
        }
        LocationList.add(locUpdate);
        para.gpsStatus = (provider + dots[GPSfired%4]);
        GPSfired += 1;
    }

    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
