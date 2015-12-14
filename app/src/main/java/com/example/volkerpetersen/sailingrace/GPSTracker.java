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
 *-----------------------------------------------------------------------------------------
 * GPS location, heading, and speed service
 *-----------------------------------------------------------------------------------------
 */
public class GPSTracker extends Service implements LocationListener {
    private GeomagneticField geoField;
    private Context appContext;
    private boolean isGPSEnabled = false;
    private boolean canGetLocation = false;
    private int GPSfired;
    private int keepNumPositions;
    private double toKts = 1.94384;         // m/s to knots conversation factor
    private GlobalParameters para;          // class object for the global parameters
    private Location location;
    private LocationManager locationManager;
    public LinkedList<Location> LocationList = new LinkedList<Location>();
    private String[] dots;
    static final String LOG_TAG = GPSTracker.class.getSimpleName();

    /**
     * GPS location, heading, and speed service
     *
     * @param appContext - Application Context
     * @param gpsUpdates - long minimum time (in milliseconds) between GPS updates
     * @param gpsDistance - double gpsDistance: minimum distance (in meters) between GPS updates
     * @param keepNumPositions - int keepNumPositions: max GPS location history to compute average speed / heading
     *
     */
    public GPSTracker(Context appContext, long gpsUpdates, double gpsDistance, int keepNumPositions) {
        this.appContext = appContext;
        this.keepNumPositions = keepNumPositions;

        // initialize our Global Parameter class by calling the
        // Application class (see application tag in AndroidManifest.xml)
        para = (GlobalParameters) appContext;

        location = getLocation(gpsUpdates, (float)gpsDistance);
        GPSfired = 0;
        dots = new String[]{".   ", "..  ", "... ", "...."};
        if (location != null) {
            geoField = new GeomagneticField(
                    Double.valueOf(location.getLatitude()).floatValue(),
                    Double.valueOf(location.getLongitude()).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );
            para.setDeclination(geoField.getDeclination());
        } else {
            para.setDeclination(0.0d);
        }
    }

    /**
     * Fetches the current Location
     * @param minGPSUpdateTime in milliseconds
     * @param minGPSDistance in meter
     * @return Location
     */
    public Location getLocation(long minGPSUpdateTime, float minGPSDistance) {
        Location loc = null;
        try {
            locationManager = (LocationManager) appContext.getSystemService(LOCATION_SERVICE);
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
                        para.setBoatLat(this.getLatitude());
                        para.setBoatLon(this.getLongitude());
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

    /**
     * Method to stop the current GPS location thread
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(GPSTracker.this);
            } catch (SecurityException e) {
                Log.e(LOG_TAG,"Location PERMISSION_NOT_GRANTED by user");
            }
        }
    }

    /**
     * Method to return the current Latitude
     * The result will be stored in the global parameter class para.latitude
     * @return double latitude
     */
    public double getLatitude() {
        if (location != null) para.setBoatLat(location.getLatitude());
        return para.getBoatLat();
    }

    /**
     * Method to return the current Longitude
     * The result will be stored in the global parameter class para.longitude
     * @return double longitude
     */
    public double getLongitude() {
        if (location != null) para.setBoatLon(location.getLongitude());
        return para.getBoatLon();
    }

    /**
     * Method to compute the current speed in nm per hour (kts)
     * The result will be stored in the global parameter class para.SOG
     */
    public void getSpeed() {
        if (location != null) para.setSOG(location.getSpeed() * toKts);
    }

    /**
     * Method to compute the current heading in True North
     * The result will be stored in the global parameter class para.COG
     */
    public void getHeading() {
        if (location != null) {
            para.setCOG(location.getBearing());
        }
    }

    /**
     * Method to compute the average speed and heading from the past (Settings History) values in
     * the FIFO queues for the SOG (kts) and COG (True North).
     * The results will be stored in the global parameter class para.avgSOG and para.avgCOG.
     */
    public void getAvgSpeedHeading() {
        if (location != null && LocationList.size()>2 ) {
            Location first = LocationList.getFirst();
            Location last = LocationList.getLast();

            para.setAvgCOG(NavigationTools.fixAngle(first.bearingTo(last)));
            double distance = first.distanceTo(last);
            double time = (last.getElapsedRealtimeNanos() - first.getElapsedRealtimeNanos()) / 1000000000.0;
            if (time != 0.0) {
                para.setAvgSOG(distance / time * toKts);
            }
        } else {
            para.setAvgCOG(para.getCOG());
            para.setAvgSOG(para.getSOG());
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
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(appContext);

        alertDialog.setTitle("GPS warning");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to the settings menu and enable GPS?");

        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                appContext.startActivity(intent);
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
        para.setGpsStatus(provider + dots[GPSfired%4]);
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
