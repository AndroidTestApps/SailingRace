package com.example.volkerpetersen.sailingrace;
/**
 * Created by Volker Petersen on November 2015.
 *
 * all angles are assumed to be True North
 */
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import android.content.Context;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private double boatLat;
    private double boatLon;
    private double markerLat;
    private double markerLon;
    private double[] toMark = new double[2];
    private double[] Laylines = new double[4];
    private GlobalParameters para;
    private Context appContext;
    public final String LOG_TAG = MapsActivity.class.getSimpleName();

    Intent intent = new Intent();
    ActionBar actionBarFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        appContext = getApplicationContext();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_xml);
        mapFragment.getMapAsync(this);

        // initialize our Global Parameter class by calling the
        // Application class (see application tag in AndroidManifest.xml)
        para = (GlobalParameters) getApplicationContext();

        actionBarFragment = getActionBar();
    }

    @Override
    public void onDestroy() { super.onDestroy(); }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        // TODO clean up this map activity
        // Log.d(LOG_TAG, "Found back button in Fragment!!!");

        // get ready to initialize the return data which will be received by the method
        // onActivityResult in the host Activity (start_raceActivity) that initiated this MapsActivit

        para.setBestTack(NavigationTools.LaylinesString(Laylines[2]));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        boolean showLaylines = false;
        String markerName;
        mMap = googleMap;
        LatLng windwardMarker = null;
        LatLng leewardMarker = null;
        LatLng activeMarker = null;
        LatLng boat = null;

        // fetch the global variables
        final double windwardLat = para.getWindwardLat();
        double windwardLon = para.getWindwardLon();
        boolean windwardRace = para.getWindwardRace();

        double leewardLat = para.getLeewardLat();
        double leewardLon = para.getLeewardLon();
        boolean leewardRace = para.getLeewardRace();

        boatLat = para.getBoatLat();
        boatLon = para.getBoatLon();

        double TWD = para.getTWD();
        double CourseOffset = para.getCourseOffset();
        double TackGybe = para.getTackGybe();
        String tack = para.getTack();
        int boatResource;
        boolean tack_position_ok = false;

        if ( !(Double.isNaN(windwardLat) || Double.isNaN(windwardLon)) && windwardRace ) {
            windwardMarker = new LatLng(windwardLat, windwardLon);
            if ( !(Double.isNaN(leewardLat) || Double.isNaN(leewardLon))) {
                leewardMarker = new LatLng(leewardLat, leewardLon);
            }
            showLaylines = true;
        }

        if ( !(Double.isNaN(leewardLat) || Double.isNaN(leewardLon)) && leewardRace ) {
            leewardMarker = new LatLng(leewardLat, leewardLon);
            if ( !(Double.isNaN(windwardLat) || Double.isNaN(windwardLon))) {
                windwardMarker = new LatLng(windwardLat, windwardLon);
            }
            showLaylines = true;
        }

        if (CourseOffset == 0.0f) {
            markerLat = windwardLat;
            markerLon = windwardLon;
            activeMarker = windwardMarker;
            markerName = "Windward Mark";
        } else if (CourseOffset == 180.0f) {
            markerLat = leewardLat;
            markerLon = leewardLon;
            activeMarker = leewardMarker;
            markerName = "Leeward Mark";
        } else {
            markerName = "Windward Mark";
            showLaylines = false;
        }

        if (!showLaylines) {
            // we've no markers set and hence can't compute laylines yet.  The default marker
            // will be set at the current boat location
            markerLat = boatLat;
            markerLon = boatLon;
            windwardMarker = new LatLng(markerLat, markerLon);
        }
        boat = new LatLng(boatLat, boatLon);

        int zoomLevel = 13;
        CameraUpdate center = CameraUpdateFactory.newLatLng(boat);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoomLevel);
        if (windwardMarker != null) {
            mMap.addMarker(new MarkerOptions()
                .position(windwardMarker)
                .draggable(true)
                .title("Windward Mark")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
        if (leewardMarker != null) {
            mMap.addMarker(new MarkerOptions()
                .position(leewardMarker)
                .draggable(true)
                .title("Leeward Mark")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        if ( showLaylines ) {
            //create a 35 pixel wide boat;
            double meters = 35 * 156543.03392 * Math.cos(boatLat * Math.PI / 180) / Math.pow(2, zoomLevel);
            // only add Boat and course laylines to map when we're in active race
            // Add an overlay to the map, retaining a handle to the GroundOverlay object.

            // compute and plot the laylines
            Laylines = NavigationTools.optimumLaylines(boatLat, boatLon, markerLat, markerLon, TWD, para.getCourseOffset(), TackGybe, tack);
            LatLng tack_position = new LatLng(markerLat, markerLon);
            if (Laylines[0] != Double.NaN && Laylines[1] != Double.NaN) {
                tack_position = new LatLng(Laylines[0], Laylines[1]);
                tack_position_ok = true;
            }

            if (Laylines[2] == 0.0d) {
                if (CourseOffset == 180.0f) {
                    boatResource = R.drawable.boatgreen_black_reach;
                } else {
                    boatResource = R.drawable.boatgreen_black;
                }
                if (tack_position_ok) {
                    mMap.addPolyline(new PolylineOptions()
                        .add(boat, tack_position)
                        .width(12)
                        .color(Color.GREEN));
                    mMap.addPolyline(new PolylineOptions()
                        .add(tack_position, activeMarker)
                        .width(12)
                        .color(Color.RED));
                }
            } else {
                if (CourseOffset == 180.0f) {
                    boatResource = R.drawable.boatred_black_reach;
                } else {
                    boatResource = R.drawable.boatred_black;
                }
                if (tack_position_ok) {
                    mMap.addPolyline(new PolylineOptions()
                        .add(boat, tack_position)
                        .width(12)
                        .color(Color.RED));
                    mMap.addPolyline(new PolylineOptions()
                        .add(tack_position, activeMarker)
                        .width(12)
                        .color(Color.GREEN));
                }
            }
            // add a Wind-arrow to the Laylines
            double[] windMarker = NavigationTools.withDistanceBearingToPosition(boatLat, boatLon, 1.1, TWD);
            /*
            Log.d(LOG_TAG, "Boat Lat:" + appContext.getString(R.string.DF3, boatLat));
            Log.d(LOG_TAG, "Boat Lon:" + appContext.getString(R.string.DF3, boatLon));
            Log.d(LOG_TAG, "DTM*1.1 :" + appContext.getString(R.string.DF3, 1.1));
            Log.d(LOG_TAG, "TWD     :" + appContext.getString(R.string.DF3, TWD));
            Log.d(LOG_TAG, "Wind Lat:" + appContext.getString(R.string.DF3, windMarker[0]));
            Log.d(LOG_TAG, "Wind Lon:" + appContext.getString(R.string.DF3, windMarker[1]));
            */
            mMap.addPolyline(new PolylineOptions()
                .add(boat, new LatLng(windMarker[0], windMarker[1]))
                .width(8)
                .color(Color.BLACK));

            // plot the boat
            GroundOverlayOptions boatImage = new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromResource(boatResource))
                    .anchor(0.5f, 0.0f)  // anchor at top of image, 50% between left and right edge
                    .position(boat, (float)meters);   // given width, height is proportionally scaled

            GroundOverlay imageOverlay = mMap.addGroundOverlay(boatImage);
            imageOverlay.setBearing((float)Laylines[3]);
            /*
            Log.d(LOG_TAG, "Boat Lat:   "+appContext.getString(R.string.DF3, boatLat));
            Log.d(LOG_TAG, "Boat Lon:   "+appContext.getString(R.string.DF3, boatLon));
            Log.d(LOG_TAG, "Tack Lat:   "+appContext.getString(R.string.DF3, Laylines[0]));
            Log.d(LOG_TAG, "Tack Lon:   "+appContext.getString(R.string.DF3, Laylines[1]));
            Log.d(LOG_TAG, "Marker Lat: "+appContext.getString(R.string.DF3, markerLat));
            Log.d(LOG_TAG, "Marker Lon: "+appContext.getString(R.string.DF3, markerLon));
            */
            actionBarFragment.setTitle("Optimum course to "+markerName);
        }

        // add a Listener to Marker to allow Marker to be moved
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker markerDragStart) {
                // TODO Auto-generated method stub
                //Log.d(LOG_TAG, "Start Google Map Marker drag");
            }

            @Override
            public void onMarkerDrag(Marker markerDrag) {
                markerLat = markerDrag.getPosition().latitude;
                markerLon = markerDrag.getPosition().longitude;
                toMark = NavigationTools.MarkDistanceBearing(boatLat, boatLon, markerLat, markerLon);
                actionBarFragment.setTitle("Windward: "+appContext.getString(R.string.BTM_DTM, toMark[1], toMark[0]));
                //Log.d(LOG_TAG, "Dragging Google Map Marker");
            }

            @Override
            public void onMarkerDragEnd(Marker markerDragEnd) {
                markerLat = markerDragEnd.getPosition().latitude;
                markerLon = markerDragEnd.getPosition().longitude;

                toMark = NavigationTools.MarkDistanceBearing(boatLat, boatLon, markerLat, markerLon);
                actionBarFragment.setTitle("Windward: "+appContext.getString(R.string.BTM_DTM, toMark[1], toMark[0]));

                // store Windward marker position in global parameters
                para.setWindwardLat(markerLat);
                para.setWindwardLon(markerLon);
                para.setWindwardFlag(true);
                //Log.d(LOG_TAG, "End Marker drag lat: " + appContext.getString(R.string.DF3, markerLat) + "  lon: " + appContext.getString(R.string.DF3, markerLon));
            }
        });
    }
}
