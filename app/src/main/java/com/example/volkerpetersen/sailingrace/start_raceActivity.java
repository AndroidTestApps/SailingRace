package com.example.volkerpetersen.sailingrace;
/**
 * Created by Volker Petersen on November 2015.
 *
 * integrates the wind data from the SailTimerWind Bluetooth Windex when key_Windex = windex = 1
 *
 * all angles used in computations are assumed to be True North.
 * all angles output to screen are Magnetic North (True North + declination)
 *
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DecimalFormat;

public class start_raceActivity extends Activity {
    private TextView outputStatus;              // output field for the GPS status
    private TextView outputLatitude;
    private TextView outputLongitude;
    private TextView outputCOG;                 // Course Over Ground output field
    private TextView outputSOG;                 // Spped Over Ground output field
    private TextView outputVMG;                 // Velocity Made Good toward the currently active mark
    private TextView outputMWD;                 // Mean Wind Direction output field
    private TextView outputMeanHeadingTarget;   // output field top of screen with MeanHeadingTarget
    private TextView outputHeaderLift;          // output field showing if historically we have been LIFTED or HEADED
    private TextView outputAvgVariance;         // output field for the historical average amount Lifted or Headed
    private TextView outputVarDuration;         // output field for the duration of the current average
    private TextView outputDTM;                 // output field for DTM (Distance-To-Mark)
    private TextView outputBTM;                 // output field for BTM (Bearing-To-Mark)
    private TextView outputTWA;                 // output field for TWA (True Wind Angle)
    private TextView outputTWS;                 // output field for BTM (True Wind Speed)
    private TextView outputAWA;                 // output field for BTM (Apparent Wind Angle)
    private TextView outputTWD;                 // output field for TWD (True Wind Direction)
    private TextView outputPrefTack;            // output filed for the Preferred Tack to current Mark
    private TextView outputWindwardMark;
    private TextView outputLeewardMark;
    private Button btnSetLeewardMark;
    private Button btnGoLeewardMark;
    private Button btnSetWindwardMark;
    private Button btnGoWindwardMark;
    private Button btnMinusTen;
    private Button btnMinusOne;
    private Button btnPlusTen;
    private Button btnPlusOne;
    private ImageView boat;
    private ImageView leftBar;
    private ImageView rightBar;
    private Handler ScreenUpdate = new Handler();   // Handler to implement the Runnable for the Screen Updates
    private GestureDetectorCompat swipeDetector;    //  swipe detector listener in Class file swipeDetector
    private GPSTracker gps;                 // gps class object
    private GlobalParameters para;          // class object for the global parameters
    private double apparentWindAngle=0.0;   // (true) compute from the windex data
    private double trueWindDirection=60.0;  // (true) compute from the windex data
    private double trueWindAngle=0.0;       // (true) compute from the windex data
    private double trueWindSpeed=5.0;       // (kts) compute from the windex data
    private double meanHeadingTarget;       // Mean Heading Target either manual goal or current avg COG
    private double meanWindDirection;       // calculated by adding tackAngle / gybeAngle to COG
    private double timeCounter = 0.0;       // keeps duration of current header / lift sequence
    private double sumVariances = 0.0;      // sum of the heading variances while in a Header or Lift sequence
    private double lastAvgVariance = 0.0;   // keeps track of the last avg variance between meanHeadingTarget and COG
    private int history;                    // number of past screen update values kept in FIFO queue
    private int screenUpdates;              // screen update frequency in sec.  Set in preferences.
    private String tack = "stbd";           // "stbd" or "port" depending on current active board we're sailing on
    private String prefTack;                // preferred tack computed for the Laylines to current Mark
    private float CourseOffset=(float)0.0;  // Upwind leg=0.0  |  Downwind leg=180.0
    private float TackGybe;                 // contains tackAngle Upwind and gybeAngle Downwind
    private float tackAngle;                // upwind leg tack angle set in preferences
    private float gybeAngle;                // downwind leg gybe angle set in preferences
    private double[] DistanceBearing = new double[2];
    private int counter = 0;
    private int windex;                     // from SharedPreferences to indicate if Windex is used (1) or not (0)
    private DecimalFormat dfThree = new DecimalFormat("000");
    private DecimalFormat dfTwo = new DecimalFormat("00");
    private DecimalFormat dfOne = new DecimalFormat("#");
    private DecimalFormat df2 = new DecimalFormat("#0.00");
    private DecimalFormat df1 = new DecimalFormat("#0.0");
    private AlertDialog alert;
    public Context context;
    static final String LOG_TAG = start_raceActivity.class.getSimpleName();
    static final int GET_MAP_MARKER_POSITION = 1;   // Our request code to pass data back from map FragmentActivity
    static final int RESULTS_OK = 1;                // Code to indicate correct results
    static final int RESULTS_CANCELED = 0;          // Code to indicate correct results
    private ColorStateList WHITE;
    private ColorStateList RED;
    private ColorStateList GREEN;
    private fifoQueueDouble TWSfifo;
    private fifoQueueDouble TWAfifo;
    private fifoQueueDouble TWDfifo;
    private fifoQueueDouble COGfifo;
    private fifoQueueDouble SOGfifo;
    private double TWSinc = 3.0;
    private double TWAinc = 5.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = getApplicationContext();

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        swipeDetector = new GestureDetectorCompat(this,new MySwipeListener());

        // fetch the Shared Preferences from the class FetchPreferenceValues in file SailingRacePreferences
        // Shared Preferences key names are defined in SailingRacePreferences.OnCreate()
        screenUpdates = SailingRacePreferences.FetchPreferenceValue("key_ScreenUpdates", context); // Time interval for screen update in seconds.
        history = SailingRacePreferences.FetchPreferenceValue("key_history", context); // max number of location positions stored in LinkedList.
        long gpsUpdates = (long) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateTime", context); // Time Interval for GPS position updates in milliseconds
        float minDistance = (float) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateDistance", context); // min distance (m) between GPS updates
        tackAngle = (float) SailingRacePreferences.FetchPreferenceValue("key_TackAngle", context);  // tack angle
        gybeAngle = (float) SailingRacePreferences.FetchPreferenceValue("key_GybeAngle", context); // gybe angle
        windex = SailingRacePreferences.FetchPreferenceValue("key_Windex", context); // boolean value to indicate if app is using / not using the Bluetooth connected Windex
        TackGybe = tackAngle;

        if (windex == 1) {
            // we have a windex connected.  So, we'll use layout with the wind data
            setContentView(R.layout.activity_start_race_windex);
        } else {
            // use layout w/o wind data
            setContentView(R.layout.activity_start_race);
        }
        setTitle("SailingRace - Racing");

        /* Debugging log statements
        Log.d(LOG_TAG, "tack Angle:" + Float.toString(tackAngle));
        Log.d(LOG_TAG, "gybe Angle:" + Float.toString(gybeAngle));
        Log.d(LOG_TAG, "History:   " + Integer.toString(history));
        Log.d(LOG_TAG, "gpsUpdate: " + Long.toString(gpsUpdates));
        Log.d(LOG_TAG, "Windex: " + Integer.toString(windex));
        */

        // fetch all the display elements from the xml file
        outputStatus = (TextView) findViewById(R.id.Status);
        outputLatitude = (TextView) findViewById(R.id.Latitude);
        outputLongitude = (TextView) findViewById(R.id.Longitude);
        outputCOG = (TextView) findViewById(R.id.COG);
        outputSOG = (TextView) findViewById(R.id.SOG);
        outputVMG = (TextView) findViewById(R.id.VMG);
        outputMWD = (TextView) findViewById(R.id.MWD);
        outputBTM = (TextView) findViewById(R.id.BTM);
        outputDTM = (TextView) findViewById(R.id.DTM);
        outputHeaderLift = (TextView) findViewById(R.id.HeaderLift);
        outputAvgVariance = (TextView) findViewById(R.id.avgVariance);
        outputVarDuration = (TextView) findViewById(R.id.varDuration);
        boat = (ImageView) findViewById(R.id.boat);
        leftBar = (ImageView) findViewById(R.id.leftBar);
        rightBar = (ImageView) findViewById(R.id.rightBar);

        if (windex == 0) {
            // SailtimerWind instrument not enabled in SharedPreferences
            outputMeanHeadingTarget = (TextView) findViewById(R.id.MeanHeadingTarget);
            btnMinusTen = (Button) findViewById(R.id.buttonMinus10);
            btnMinusOne = (Button) findViewById(R.id.buttonMinus);
            btnPlusTen = (Button) findViewById(R.id.buttonPlus10);
            btnPlusOne = (Button) findViewById(R.id.buttonPlus);
            btnSetLeewardMark = (Button) findViewById(R.id.buttonSetLeewardMark);
            btnGoLeewardMark = (Button) findViewById(R.id.buttonGoLeewardMark);
            btnSetWindwardMark = (Button) findViewById(R.id.buttonSetWindwardMark);
            btnGoWindwardMark = (Button) findViewById(R.id.buttonGoWindwardMark);
            meanHeadingTarget = Integer.parseInt(outputMeanHeadingTarget.getText().toString());
            btnGoLeewardMark.setEnabled(false);
            btnGoWindwardMark.setEnabled(false);
        } else {
            // SailtimerWind instrument is enabled in SharedPreferences
            outputTWA = (TextView) findViewById(R.id.TWA);
            outputTWS = (TextView) findViewById(R.id.TWS);
            outputAWA = (TextView) findViewById(R.id.AWA);
            outputTWD = (TextView) findViewById(R.id.TWD);
            outputWindwardMark = (TextView) findViewById(R.id.WindwardMark);
            outputLeewardMark = (TextView) findViewById(R.id.LeewardMark);
            outputPrefTack = (TextView) findViewById(R.id.PrefTack);
        }

        // initialize the color variables (type ColorStateList)
        WHITE = ContextCompat.getColorStateList(context, R.color.WHITE);
        RED = ContextCompat.getColorStateList(context, R.color.RED);
        GREEN = ContextCompat.getColorStateList(context, R.color.GREEN);

        // initialize the GPS tracker and ScreenUpdate
        gps = new GPSTracker(start_raceActivity.this, para, gpsUpdates, minDistance, history);
        ScreenUpdate.post(updateScreenNow);

        // initialize our Global Parameter class
        para = new GlobalParameters();
        //Log.d(LOG_TAG, "start_raceActivity value of 'leewardSet': "+para.leewardSet);
        //Log.d(LOG_TAG, "start_raceActivity value of 'windwardSet': "+para.windwardSet);

        if (para.leewardSet) {
            para.leewardSet = false;
            setLeewardMark();
        }
        if (para.windwardSet) {
            para.windwardSet = false;
            setWindwardMark(true);
            goWindwardMark();
        }

        // initialize the FIFO queues to keep history for TWA, TWS, COG, SOG
        TWDfifo = new fifoQueueDouble(history);
        TWAfifo = new fifoQueueDouble(history);
        TWSfifo = new fifoQueueDouble(history);
        COGfifo = new fifoQueueDouble(history);
        SOGfifo = new fifoQueueDouble(history);
        prefTack = "- -";

        /* Test the FIFO queue class
        Log.d(LOG_TAG, "FIFI Queue size: " + dfOne.format(history));
        TWAfifo.add(3.0);
        TWAfifo.add(6.0);
        Log.d(LOG_TAG, "Average of 3, 6: " + df2.format(TWAfifo.average()));
        TWAfifo.logQueue();
        TWAfifo.add(9.0);
        TWAfifo.add(12.0);
        Log.d(LOG_TAG, "current queue elements (sorted First-In to Last-In");
        TWAfifo.logQueue();
        Log.d(LOG_TAG, "Average of 6, 9, 12: " + df2.format(TWAfifo.average()));
        Log.d(LOG_TAG, "First value in FIFO: " + df2.format(TWAfifo.getFirst()));
        TWAfifo.logQueue();
        Log.d(LOG_TAG, "Last value in FIFO: " + df2.format(TWAfifo.getLast()));
        // End of FIFO Queue test
        */

        boat.setOnClickListener(new View.OnClickListener() {
            // listener to detect button press on the boat.  When pressed we're going to tack/jibe
            @Override
            public void onClick(View view) {
                if (view == findViewById(R.id.boat)) {
                    readyToTackOrGybe();
                }
            }
        });

        boat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // if we have a long Click, we want to change from Upwind to Downwind or visa versa
                CourseOffset = (float) 180.0 - CourseOffset;
                if (CourseOffset == 0.0) {
                    TackGybe = tackAngle;
                } else {
                    TackGybe = -gybeAngle;
                }
                updateMarkerButtons();
                //alertDialog.setMessage("We detected a long click on the boat image!");
                //alertDialog.show();
                //Toast toast = Toast.makeText(context, "long Click on Boat", Toast.LENGTH_LONG);
                //toast.setGravity(Gravity.TOP| Gravity.CENTER, 0, 0);
                //toast.show();
                return true;
            }
        });

        // initialize buttons only for the No-Windex option
        if (windex == 0) {
            btnSetLeewardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go Set or Clear the Leeward mark
                    setLeewardMark();
                }
            });

            btnSetWindwardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go Set or Clear the Windward mark
                    setWindwardMark(false);
                }
            });

            btnSetWindwardMark.setOnLongClickListener(new View.OnLongClickListener() {
                // if we have a long Click, we want to go to Google Maps to set the mark on the map
                @Override
                public boolean onLongClick(View view) {
                    if (para.windwardSet) {
                        return true;
                    }
                    // creating a bundle object to pass data to the Google Maps Fragment (MapsActivity)
                    Bundle bundle = new Bundle();
                    bundle.putDouble("boatLat", para.latitude);
                    bundle.putDouble("boatLon", para.longitude);
                    bundle.putDouble("markerLat", Double.NaN);
                    bundle.putDouble("markerLon", Double.NaN);

                    Intent intent = new Intent(context, MapsActivity.class);
                    intent.putExtras(bundle);
                    // the "startActivityForResults" system method allows the calls upon Fragment to return results.
                    // the results are processed in the method onActivityResults() below.
                    startActivityForResult(intent, GET_MAP_MARKER_POSITION);
                    return true;
                }
            });

            btnGoWindwardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go set the Leeward mark
                    goWindwardMark();
                }
            });

            btnGoLeewardMark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if we have a Click, we want to go set the Leeward mark
                    goLeewardMark();
                }
            });
        }
        // end of No-Windex button initialization

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.AlertHeader);
        builder.setPositiveButton("Delete marker", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Go ahead and delete the Windward / Leeward Marker
                // id indicates if we have a request for Windward or Leeward Marker deletion
                clearPositionMark();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog. Nothing to do here
            }
        });
        // Create the AlertDialog object and return it
        alert = builder.create();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.swipeDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    // Method to implement the Swipe Gesture detection.  This class requires the above
    // method "onTouchEvent"
    class MySwipeListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            //Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final int SWIPE_MIN_DISTANCE = 120;
            final int SWIPE_MAX_OFF_PATH = 200;
            final int SWIPE_THRESHOLD_VELOCITY = 200;
            boolean swipe = false;
            try {
                float diffAbs = Math.abs(e1.getY() - e2.getY());
                float diff = e1.getX() - e2.getX();

                if (diffAbs > SWIPE_MAX_OFF_PATH)
                    return false;

                // Left swipe
                if (diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    swipe = true;

                    // Right swipe
                } else if (-diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    swipe = true;
                }
            } catch (Exception e) {
                Log.d("YourActivity", "Error on gestures");
            }
            if (swipe) {
                changeActivityOnSwipe();
            }
            return false;
        }
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(LOG_TAG, "Back pressed");
            confirmQuit();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            gps.stopUsingGPS();     // stop the GPS thread
            ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // set screen time out to on
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error on Destroy: "+e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // method that retrieves the data which has been returned from the Google Maps FragmentActivity (MapsActivity)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check which request we're responding to.  Here we're interested in the Google Maps marker position
        ScreenUpdate.post(updateScreenNow);     // restart the Screen Updates
        if (requestCode == GET_MAP_MARKER_POSITION) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // a Marker position has been set.  Make that our Windward mark.
                Bundle extras = intent.getExtras();
                para.windwardLAT = extras.getDouble("markerLat");
                para.windwardLON = extras.getDouble("markerLon");
                para.windwardSet = false;
                prefTack = extras.getString("PrefTack");
                setWindwardMark(true);
                goWindwardMark();
                DistanceBearing = NavigationTools.MarkDistanceBearing(para.latitude, para.longitude, para.windwardLAT, para.windwardLON);
                //Log.d(LOG_TAG, "onActivityResult DTM:" + df2.format(DistanceBearing[0]));
                //Log.d(LOG_TAG, "onActivityResult BTM:" + df1.format(DistanceBearing[1]) + "°");
            } else {
                // TODO here any error handling for resultCode != RESULT_OK
                //Log.d(LOG_TAG, "onActivityResult was closed w/o setting the marker position");
            }
        }
        //Log.d(LOG_TAG, "onActivityResult resultCode = " + resultCode + " requestCode " + requestCode);
    }

    // Method execute the desired action after the Class "swipeDetector" detected a left or right swipe
    public void changeActivityOnSwipe() {
        // creating a bundle object to pass data to the Google Maps Fragment (MapsActivity)
        ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
        Bundle bundle = new Bundle();
        bundle.putBoolean("windwardRace", para.windwardRace);
        bundle.putBoolean("leewardRace", para.leewardRace);
        bundle.putDouble("windwardLat", para.windwardLAT);
        bundle.putDouble("windwardLon", para.windwardLON);
        bundle.putDouble("leewardLat", para.leewardLAT);
        bundle.putDouble("leewardLon", para.leewardLON);
        bundle.putDouble("boatLat", para.latitude);
        bundle.putDouble("boatLon", para.longitude);
        bundle.putDouble("COG", para.COG);
        bundle.putDouble("TWD", TWDfifo.average());
        bundle.putDouble("CourseOffset", CourseOffset);
        bundle.putString("tack", tack);
        bundle.putDouble("TackGybe", TackGybe);

        Intent intent = new Intent(context, MapsActivity.class);
        intent.putExtras(bundle);
        // the "startActivityForResults" system method allows the calls upon Fragment to return results.
        // the results are processed in the method onActivityResults() below.
        startActivityForResult(intent, GET_MAP_MARKER_POSITION);
    }

    // Method to handle the "Quit" confirmation
    private void confirmQuit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.QuitRace);
        builder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog,int id){
                // Go ahead quit this race
                finish();
            }
        });
        builder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog,int id){
                // User cancelled the dialog. Go ahead and continue the app
                onResume();
             }
        });
        // Create the AlertDialog object and return it
        alert=builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_race, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SailingRacePreferences.class));
                return true;
            case R.id.timer_mode:
                startActivity(new Intent(this, start_timerActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //-----------------------------------------------------------------------------------------
    // Runnable to update the display every "screenUpdates" seconds
    //-----------------------------------------------------------------------------------------
    Runnable updateScreenNow = new Runnable() {
        public void run() {
            String tmp;
            double delta;

            if (gps.canGetLocation()) {
                para.latitude = gps.getLatitude();
                para.longitude = gps.getLongitude();
                gps.getHeading();
                gps.getSpeed();
                gps.getAvgSpeedHeading();
                counter = (counter + 1);
            } else {
                gps.showSettingsAlert();
            }

            // if we have an active Leeward or Windward mark, compute distance and Bearing to mark
            if (para.leewardRace) {
                DistanceBearing = NavigationTools.MarkDistanceBearing(para.latitude, para.longitude, para.leewardLAT, para.leewardLON);
            }
            if (para.windwardRace) {
                DistanceBearing = NavigationTools.MarkDistanceBearing(para.latitude, para.longitude, para.windwardLAT, para.windwardLON);
            }

            if (para.leewardRace || para.windwardRace) {
                outputDTM.setText(df2.format(DistanceBearing[0]));          // update DTM (Distance-To-Mark) in nm
                tmp = dfOne.format(NavigationTools.fixAngle(DistanceBearing[1]+para.declination)) + "°";
                outputBTM.setText(tmp);  // update BTM (Bearing-To-Mark)
                // computing meanHeadingTarget which now is theoretical BTM plus TackGybe angle
                if (tack.equals("stbd")) {
                    meanHeadingTarget = NavigationTools.fixAngle(DistanceBearing[1] - TackGybe + para.declination);
                } else {
                    meanHeadingTarget = NavigationTools.fixAngle(DistanceBearing[1] + TackGybe + para.declination);
                }
            }

            if (windex == 1) {
                // dummy wind data
                trueWindDirection      = NavigationTools.fixAngle(trueWindDirection + TWAinc);
                apparentWindAngle  = (apparentWindAngle + TWAinc);
                trueWindSpeed     += TWSinc;
                if (trueWindSpeed >= 25.0 || trueWindSpeed <= 5.0) {
                    TWSinc = 0.0 - TWSinc;
                }
                if (Math.abs(trueWindDirection) >= 90.0 && TWAinc > 0.0) {
                    TWAinc = 0.0 - TWAinc;
                }
                if (Math.abs(trueWindDirection) < 40.0 && TWAinc < 0.0) {
                    TWAinc = 0.0 - TWAinc;
                }
                trueWindAngle = TackGybe + TWAinc;
                // end of dummy wind data

                TWSfifo.add(trueWindSpeed);
                TWAfifo.add(trueWindAngle);
                TWDfifo.add(trueWindDirection);
                //Log.d(LOG_TAG, "avg TWS: " + df2.format(TWSfifo.average()));
                //Log.d(LOG_TAG, "avg TWA: "+df2.format(TWAfifo.average()));

                // end of dummy wind data

                if (para.leewardRace || para.windwardRace) {
                    delta = NavigationTools.HeadingDelta(DistanceBearing[1], (trueWindDirection - CourseOffset));
                } else {
                    delta = trueWindAngle-TackGybe;
                }
                tmp = dfOne.format(NavigationTools.fixAngle(trueWindDirection + para.declination)) + "°";
                outputTWD.setText(tmp);
                tmp = dfOne.format(NavigationTools.fixAngle(trueWindAngle + para.declination)) + "°";
                outputTWA.setText(tmp);
                tmp = dfOne.format(NavigationTools.fixAngle(apparentWindAngle + para.declination)) + "°";
                outputAWA.setText(tmp);
                outputTWS.setText(df1.format(trueWindSpeed));
                outputPrefTack.setText(prefTack);
            } else {
                // compute the deviation from our "meanHeadingTarget"
                delta = NavigationTools.HeadingDelta(meanHeadingTarget, para.COG);
                outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
                tmp = dfOne.format(NavigationTools.fixAngle(meanWindDirection + para.declination))+"°";
                outputMWD.setText(tmp);
            }

            updateBarChart(delta);
            lastAvgVariance = updateVarianceSumAvg(delta, lastAvgVariance);

            COGfifo.add(para.COG);
            SOGfifo.add(para.SOG);
            //Log.d(LOG_TAG, "avg COG: " + df2.format(COGfifo.average()));
            //Log.d(LOG_TAG, "avg SOG: " + df2.format(SOGfifo.average()));

            /*
            add tack/gybe and course change detection after everything else
            has been debugged.
            checkForTackGybe();
            checkForUpwindDownwindChange();
            */

            tmp = dfOne.format(para.COG) + "°";
            outputCOG.setText(tmp);

            outputSOG.setText(df1.format(para.SOG)); // or use avgSOG
            double vmgu = NavigationTools.calcVMGu(TWAfifo.average(), CourseOffset, para.SOG);
            tmp = dfOne.format(vmgu*100.0) + "%";
            outputVMG.setText(tmp);

            outputStatus.setText(para.gpsStatus);

            tmp = "Lat:" + NavigationTools.PositionDegreeToString(para.latitude, true);
            outputLatitude.setText(tmp);

            tmp = "Lon:" + NavigationTools.PositionDegreeToString(para.longitude, false) + "  δ:" + df1.format(para.declination);
            outputLongitude.setText(tmp);

            // set the screen display frequency
            ScreenUpdate.postDelayed(this, (long) (screenUpdates * 1000));
        }
    };

    //-----------------------------------------------------------------------------------------
    // Class methods to update the heading variances SUM and AVG
    //-----------------------------------------------------------------------------------------
    public double updateVarianceSumAvg(double delta, double last) {
        double avgVariance = 0.0;
        double sign = 1.0;
        String tmp;

        // calculate the correct sign for the course / board we are currently sailing on.
        // We want a Header be negative values and Lift positive values based on the
        // current course.
        //   no SailtimerWind Instrument (windex=0): delta = meanHeadingTarget-COG
        // with SailtimerWind Instrument (windex=1): delta = COG+TWA-BTM
        //
        //           stbd     port
        //           H   L    H   L
        //          (-) (+)  (-) (+)
        // Upwind    -   +    +   -    necessary value of sign in order to yield a negative values
        // Downwind  +   -    -   +    for Header and positive values for Lift situations
        //
        if (CourseOffset == 0.0) {
            // Upwind leg (CourseOffset = 0.0)
            sign = 1.0;
        } else {
            // Downwind leg (CourseOffset = 180.0)
            sign = -1.0;
        }
        if (tack.equals("port")) {
            sign = -sign;
        }
        delta = delta * sign;
        
        // check if we still have the same deviation (<0 or >0) as last time
        if ( (last >= 0.0 && delta >= 0.0) || (last <= 0.0 && delta <= 0.0) ) {
            timeCounter = timeCounter + screenUpdates;
            sumVariances = sumVariances + delta;
            avgVariance = sumVariances / (timeCounter/screenUpdates);
        } else {
            // if not, reset timer and sum
            timeCounter = 0;
            sumVariances = 0.0;
            avgVariance = 0.0;
        }

        if (avgVariance < 0.0) {
            outputHeaderLift.setText("Header");
            outputHeaderLift.setTextColor(RED);
        } else {
            outputHeaderLift.setText("Lift");
            outputHeaderLift.setTextColor(GREEN);
        }
        tmp = dfOne.format(Math.abs(avgVariance)) + "°";
        outputAvgVariance.setText(tmp);
        tmp = dfTwo.format((long)(timeCounter/60.0))+":"+dfTwo.format(NavigationTools.Mod(timeCounter, 60.0));
        outputVarDuration.setText(tmp);

        return avgVariance;
    }
    //-----------------------------------------------------------------------------------------
    // Class methods to update the bar chart on bottom of screen
    //-----------------------------------------------------------------------------------------
    public void updateBarChart(double delta) {
        int index;
        String left;
        String right;
        if (Math.abs(delta) < 20.0) {
            index = (int)(Math.round((Math.abs(delta) % 20) / 2.0) * 2);
        } else {
            index = 20;
        }
        if ( tack.equals("stbd") ) {
            if (delta < 0.0) {
                left = "left"+dfTwo.format(index);
                right = "right00";
            } else {
                left = "left00";
                right = "right" + dfTwo.format(index);
            }
        } else {
            if (delta < 0.0) {
                left = "left"+dfTwo.format(index);
                right = "right00";
            } else {
                left = "left00";
                right = "right"+dfTwo.format(index);
            }
        }

        int leftImage = getResources().getIdentifier(left, "drawable", getPackageName());
        int rightImage = getResources().getIdentifier(right, "drawable", getPackageName());

        leftBar.setImageResource(leftImage);
        rightBar.setImageResource(rightImage);
    }

    //-----------------------------------------------------------------------------------------
    // Class methods to increase/decrease the Mean Heading target
    // initiated by UI buttons
    //-----------------------------------------------------------------------------------------
    public void decreaseMeanHeading(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 1.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 1.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void decreaseMeanHeading10(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 10.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading10(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 10.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    //-----------------------------------------------------------------------------------------
    // Class methods to set Windward and Leeward marks
    // initiated by UI buttons defined in activity_start_race and the onClick listener in onCreate
    //-----------------------------------------------------------------------------------------
    public void setWindwardMark(boolean mapMarkerSet) {
        if (!mapMarkerSet) {
            para.windwardLAT = gps.getLatitude();
            para.windwardLON = gps.getLongitude();
        }
        if (para.windwardSet) {
            // display a confirmation dialog to make sure user wants to delete the mark position
            alert.show();
        } else {
            para.windwardSet = true;
        }
        updateMarkerButtons();
    }

    public void setLeewardMark() {
        para.leewardLAT = gps.getLatitude();
        para.leewardLON = gps.getLongitude();
        if (para.leewardSet) {
            // display a confirmation dialog to make sure user wants to delete the mark position
            alert.show();
        } else {
            para.leewardSet = true;
        }
        updateMarkerButtons();
    }

    public void clearPositionMark() {
        // this method is called from the AlertDialog "alert" created in the onCreate method
        // when user confirms the deletion of a current Windward or Leeward marker we
        // will do so in this method
        if (para.windwardSet) {
            para.windwardSet = false;
            para.windwardLAT = Double.NaN;
            para.windwardLON = Double.NaN;
        }
        if (para.leewardSet) {
            para.leewardSet = false;
            para.leewardLAT = Double.NaN;
            para.leewardLON = Double.NaN;
        }
        updateMarkerButtons();
    }
    //-----------------------------------------------------------------------------------------
    // Class methods to start racing toward Windward and Leeward marks
    // initiated by UI buttons defined in activity_start_race
    //-----------------------------------------------------------------------------------------
    public void goWindwardMark() {
        if (para.windwardSet) {
            para.windwardRace = true;
            para.leewardRace = false;
            CourseOffset = (float) 0.0;     // on Upwind leg we sail to the wind, no correct required
            TackGybe = tackAngle;
        } else {
            para.windwardRace = false;
        }
        updateMarkerButtons();
    }

    public void goLeewardMark() {
        if (para.leewardSet) {
            para.leewardRace = true;
            para.windwardRace = false;
            CourseOffset = (float) 180.0;   // on Downwind leg we sail away from the wind
            TackGybe = -gybeAngle;          // since on Downwind leg the math is opposite to Upwind legs
        } else {
            para.leewardRace = false;
        }
        updateMarkerButtons();
    }

    public void updateMarkerButtons() {
        if (windex == 0) {
            // only need to worry about these button when we're in the non-Windex mode
            if (para.windwardSet || para.leewardSet) {
                // we update the button status to false (inactive) when the "markerIsSet" is set to true
                btnMinusTen.setEnabled(false);
                btnMinusOne.setEnabled(false);
                btnPlusTen.setEnabled(false);
                btnPlusOne.setEnabled(false);
            } else {
                // we update the button status to true (active) when the "markerIsSet" is set to false
                btnMinusTen.setEnabled(true);
                btnMinusOne.setEnabled(true);
                btnPlusTen.setEnabled(true);
                btnPlusOne.setEnabled(true);
            }
            if (para.leewardSet && para.leewardRace) {
                btnSetLeewardMark.setText("CLR");
                btnGoLeewardMark.setTextColor(GREEN);
            } else if (para.leewardSet) {
                btnSetLeewardMark.setText("CLR");
                btnGoLeewardMark.setTextColor(RED);
            } else {
                btnSetLeewardMark.setText("SET");
                btnGoLeewardMark.setTextColor(WHITE);
            }
            if (para.windwardSet && para.windwardRace) {
                btnSetWindwardMark.setText("CLR");
                btnGoWindwardMark.setTextColor(GREEN);
            } else if (para.windwardSet) {
                btnSetWindwardMark.setText("CLR");
                btnGoWindwardMark.setTextColor(RED);
            } else {
                btnSetWindwardMark.setText("SET");
                btnGoWindwardMark.setTextColor(WHITE);
            }
        } else {
            if (para.leewardSet && para.leewardRace) {
                outputLeewardMark.setText("Leeward Mark: GO");
                outputLeewardMark.setTextColor(GREEN);
            } else if (para.leewardSet) {
                outputLeewardMark.setText("Leeward Mark: SET");
                outputLeewardMark.setTextColor(RED);
            } else {
                outputLeewardMark.setText("Leeward Mark: Not Set");
                outputLeewardMark.setTextColor(WHITE);
            }
            if (para.windwardSet && para.windwardRace) {
                outputWindwardMark.setText("Windward Mark: GO");
                outputWindwardMark.setTextColor(GREEN);
            } else if (para.windwardSet) {
                outputWindwardMark.setText("WWD Mark: SET");
                outputWindwardMark.setTextColor(RED);
            } else {
                outputWindwardMark.setText("WWD Mark: Not Set");
                outputWindwardMark.setTextColor(WHITE);
            }
        }
        timeCounter = 0.0;
        sumVariances = 0.0;
        gps.LocationList.clear();
        gps.LocationList.clear();
        if (CourseOffset == 0.0) {
            // Upwind leg - we need to change meanHeadingTaget from downwind to upwind leg
            TackGybe = tackAngle;
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - gybeAngle - 180.0 - tackAngle);
                boat.setImageResource(R.drawable.boatgreen);
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + gybeAngle + 180.0 + tackAngle);
                boat.setImageResource(R.drawable.boatred);
            }
        } else {
            // Downwind leg - we need to change meanHeadingTaget from upwind to downwind leg
            TackGybe = -gybeAngle;          // since on Downwind leg the math is opposite to Upwind legs
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + tackAngle + 180.0 + gybeAngle);
                boat.setImageResource(R.drawable.boatgreen_reach);
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - tackAngle - 180.0 - gybeAngle);
                boat.setImageResource(R.drawable.boatred_reach);
            }
        }
    }

    //-----------------------------------------------------------------------------------------
    // Class method to compute a theoretical MWD from the meanHeadingTarget and the
    // assumed optimum tack angle
     //-----------------------------------------------------------------------------------------
    public double MWD() {
        if (tack.equals("stbd")) {
            return (NavigationTools.fixAngle(meanHeadingTarget + TackGybe + CourseOffset));
        }
        return (NavigationTools.fixAngle(meanHeadingTarget - TackGybe + CourseOffset));
    }

    //-----------------------------------------------------------------------------------------
    // Class method to detect if we tacked or gybed on current course
    //-----------------------------------------------------------------------------------------
    void checkForTackGybe() {
        if ((Math.abs(NavigationTools.HeadingDelta(meanHeadingTarget, para.COG)) > 50.0) && (Math.abs(NavigationTools.HeadingDelta(meanWindDirection, para.COG)) < 80.0)) {
            // this should indicate that we tacked / gybed
            readyToTackOrGybe();
        }
    }

    //-----------------------------------------------------------------------------------------
    // Class method to detect if we're on Upwind or Downwind leg of race
    //-----------------------------------------------------------------------------------------
    void checkForUpwindDownwindChange() {
        if (Math.abs(NavigationTools.HeadingDelta(meanWindDirection, para.COG)) > 80.0) {
            // this should indicate that we are now sailing Downwind
            CourseOffset = (float) 0.0;
        } else {
            // this should indicate that we are now sailing Upwind
            CourseOffset = (float) 180.0;
        }
        if (Math.abs(NavigationTools.HeadingDelta(meanHeadingTarget, para.COG)) > 80.0) {
            meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 180.0);
        }
        if (para.leewardRace || para.windwardRace) {
            if (CourseOffset == 180.0) {
                goWindwardMark();
            } else {
                goLeewardMark();
            }
        }
    }

    //-----------------------------------------------------------------------------------------
    // Class method to execute a tack/gybe by updating all parameters
    //-----------------------------------------------------------------------------------------
    void readyToTackOrGybe() {
        if ( tack.equals("stbd") ) {
            tack = "port";
            if (CourseOffset == 0.0) {
                // Upwind leg
                boat.setImageResource(R.drawable.boatred);
            } else {
                // Downwind leg
                boat.setImageResource(R.drawable.boatred_reach);
            }
            /*
            meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 2.0 * TackGybe);
            if (windex == 0) {
                outputMeanHeadingTarget.setTextColor(RED);
                outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
            }
            */
        } else {
            tack = "stbd";
            if (CourseOffset == 0.0) {
                // Upwind leg
                boat.setImageResource(R.drawable.boatgreen);
            } else {
                // Downwind leg
                boat.setImageResource(R.drawable.boatgreen_reach);
            }
            /*
            meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 2.0 * TackGybe);
            if (windex == 0) {
                outputMeanHeadingTarget.setTextColor(GREEN);
                outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
            }
            */
        }
        timeCounter = 0.0;
        sumVariances = 0.0;
        gps.LocationList.clear();
        updateMarkerButtons();
    }
}
