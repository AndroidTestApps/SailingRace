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

/**
 * RaceActivity class to manage all screen updates, location updates, and computations
 * during the race.  This class supports race to Windward and Leeward marks (one each) and
 * a manual wind mode (no Windex instrument) and a Windex instrument supported race mode
 */
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
    private TextView outputGoalVMG;             // output field for Goal VMG to the current Mark
    private TextView outputGoalVMGtext;         // output field for the text of the Goal VMG
    private TextView outputLeewardMark;
    private TextView outputWindwardMark;
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
    private double apparentWindAngle=0.0d;  // (true) compute from the windex data
    private double trueWindDirection=60.0d; // (true) compute from the windex data
    private double trueWindAngle=0.0d;      // (true) compute from the windex data
    private double trueWindSpeed=5.0d;      // (kts) compute from the windex data
    private double meanHeadingTarget;       // Mean Heading Target either manual goal or current avg COG
    private double meanWindDirection;       // calculated by adding tackAngle / gybeAngle to COG
    private double timeCounter = 0.0d;      // keeps duration of current header / lift sequence
    private double sumVariances = 0.0d;     // sum of the heading variances while in a Header or Lift sequence
    private double lastAvgVariance = 0.0d;  // keeps track of the last avg variance between meanHeadingTarget and COG
    private int screenUpdates;              // screen update frequency in sec.  Set in preferences.
    private String tack = "stbd";           // "stbd" or "port" depending on current active board we're sailing on
    private double CourseOffset = 0.0d;     // Upwind leg=0.0  |  Downwind leg=180.0
    private double TackGybe;                // contains tackAngle Upwind and gybeAngle Downwind
    private double tackAngle;               // upwind leg tack angle set in preferences
    private double gybeAngle;               // downwind leg gybe angle set in preferences
    private int polars;                     // =1 if the program is utilizing the build-in Capri-25 polars
    private int windex;                     // from SharedPreferences to indicate if Windex is used (1) or not (0)
    private double[] DistanceBearing = new double[2];
    private int counter = 0;
    private AlertDialog alert;
    private Context appContext;
    static final String LOG_TAG = start_raceActivity.class.getSimpleName();
    private ColorStateList WHITE;
    private ColorStateList RED;
    private ColorStateList GREEN;
    private fifoQueueDouble TWSfifo;
    private fifoQueueDouble TWAfifo;
    private fifoQueueDouble TWDfifo;
    private fifoQueueDouble COGfifo;
    private fifoQueueDouble SOGfifo;
    private double TWSinc = 3.0d;
    private double TWAinc = 5.0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int history;                    // number of past screen update values kept in FIFO queue
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appContext = getApplicationContext();

        // initialize our Global Parameter class by calling the
        // Application class (see application tag in AndroidManifest.xml)
        para = (GlobalParameters) appContext;

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        swipeDetector = new GestureDetectorCompat(appContext, new MySwipeListener());

        // fetch the Shared Preferences from the class FetchPreferenceValues in file SailingRacePreferences
        // Shared Preferences key names are defined in SailingRacePreferences.OnCreate()
        screenUpdates = SailingRacePreferences.FetchPreferenceValue("key_ScreenUpdates", appContext); // Time interval for screen update in seconds.
        history = SailingRacePreferences.FetchPreferenceValue("key_history", appContext); // max number of location positions stored in LinkedList.
        long gpsUpdates = (long) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateTime", appContext); // Time Interval for GPS position updates in milliseconds
        double minDistance = (double) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateDistance", appContext); // min distance (m) between GPS updates
        polars = SailingRacePreferences.FetchPreferenceValue("key_Polars", appContext); // =1 if we use Capri-25 build-in polars
        tackAngle = (double) SailingRacePreferences.FetchPreferenceValue("key_TackAngle", appContext);  // tack angle
        gybeAngle = (double) SailingRacePreferences.FetchPreferenceValue("key_GybeAngle", appContext); // gybe angle
        windex = SailingRacePreferences.FetchPreferenceValue("key_Windex", appContext); // boolean value to indicate if app is using / not using the Bluetooth connected Windex

        if (windex == 1) {
            // we have a windex connected.  So, we'll use layout with the wind data
            setContentView(R.layout.activity_start_race_windex);
        } else {
            // use layout w/o wind data
            setContentView(R.layout.activity_start_race);
        }
        setTitle("SailingRace - Racing");

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
            outputGoalVMG = (TextView) findViewById(R.id.GoalVMG);
            outputGoalVMGtext = (TextView) findViewById(R.id.GoalVMGtext);
            outputLeewardMark = (TextView) findViewById(R.id.LeewardMark);
            outputWindwardMark = (TextView) findViewById(R.id.WindwardMark);
        }

        // initialize the color variables (type ColorStateList)
        WHITE = ContextCompat.getColorStateList(appContext, R.color.WHITE);
        RED = ContextCompat.getColorStateList(appContext, R.color.RED);
        GREEN = ContextCompat.getColorStateList(appContext, R.color.GREEN);

        // initialize the GPS tracker and ScreenUpdate
        gps = new GPSTracker(appContext, gpsUpdates, minDistance, history);
        ScreenUpdate.post(updateScreenNow);
        TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, trueWindSpeed);
        para.setTackGybe(TackGybe);

        // testing only
        Log.d(LOG_TAG, "++++++++++++++++++++++++ Testing code  ++++++++++++++++++++++++");
        if(gps.canGetLocation()) {
            para.setBoatLat(gps.getLatitude());
            para.setBoatLon(gps.getLongitude());
            double[] mark = NavigationTools.withDistanceBearingToPosition(para.getBoatLat(), para.getBoatLon(), 1.3d, 60.0d);
            para.setWindwardLat(mark[0]);
            para.setWindwardLon(mark[1]);
            para.setWindwardFlag(true);
            mark = NavigationTools.withDistanceBearingToPosition(para.getBoatLat(), para.getBoatLon(), 1.3d, 240.0d);
            para.setLeewardLat(mark[0]);
            para.setLeewardLon(mark[1]);
            para.setLeewardFlag(true);
            goWindwardMark();
        }
        /*
        double i = 3.0d;
        double p[] = new double[2];
        Log.d(LOG_TAG, "++++++ Polars Test");
        Log.d(LOG_TAG, "Course     TWS   TBS   Tack Angle");
        while (i < 16.0d) {
            p = NavigationTools.getPolars(i, 0.0d);
            Log.d(LOG_TAG, "Windward   "+appContext.getString(R.string.DF2, i) + "   " + appContext.getString(R.string.DF2, p[0]) + "   "+appContext.getString(R.string.DF2, p[1]));
            p = NavigationTools.getPolars(i, 180.0d);
            Log.d(LOG_TAG, "Leeward    " +appContext.getString(R.string.DF2, i) + "   " + appContext.getString(R.string.DF2, p[0]) + "   "+appContext.getString(R.string.DF2, p[1]));
            i = i + 3.0;
        }
        */
        // //end of testing code

        // initialize the FIFO queues to keep history for TWA, TWS, COG, SOG
        TWDfifo = new fifoQueueDouble(history);
        TWAfifo = new fifoQueueDouble(history);
        TWSfifo = new fifoQueueDouble(history);
        COGfifo = new fifoQueueDouble(history);
        SOGfifo = new fifoQueueDouble(history);
        para.setBestTack("- -");

        // check for a Click on Boat to see if we want to Tack or Gybe
        boat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (view == findViewById(R.id.boat)) {
                if (tack.equals("stbd")) {
                    tack = "port";
                } else {
                    tack = "stbd";
                }
                updateMarkerButtons();
            }
            }
        });

        // check for a long Click on Boat to see if we want to change from Upwind to Downwind or visa versa
        boat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
            CourseOffset = 180.0d - CourseOffset;
            para.setCourseOffset(CourseOffset);
            if (CourseOffset == 0.0d) {
                goWindwardMark();
            } else {
                goLeewardMark();
            }
            return true;
            }
        });

        /**
         * initialize buttons only for the No-Windex option
         */
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
                // check if the Windward Mark has been set (WindwardFlag=true)
                if (para.getWindwardFlag()) {
                    return true;
                }
                startActivity(new Intent(appContext, MapsActivity.class));
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

    /**
     * Initialize Touch Events to be able to find Screen Swipes
     * @param MotionEvent event
     * @return onTouchEvent
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.swipeDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    /**
     * MySwipeListener Method to detect Swipe Gestures.  This class requires the above
     * method "onTouchEvent"
     */
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

        /*
        Log.d(LOG_TAG, "+++++++++++++++++++++++++++++ On Resume ++++++++++++++++++++++++++++++++");
        Log.d(LOG_TAG, "Windward Lat  " + appContext.getString(R.string.DF3, para.getWindwardLat()));
        Log.d(LOG_TAG, "Windward Race " + para.getWindwardRace());
        Log.d(LOG_TAG, "Windward Flag " + para.getWindwardFlag());
        Log.d(LOG_TAG, "Leeward Lat   " + appContext.getString(R.string.DF3, para.getLeewardLat()));
        Log.d(LOG_TAG, "Boat Lat      " + appContext.getString(R.string.DF3, para.getBoatLat()));
        Log.d(LOG_TAG, "para          " + para);
        */

        if (para.getWindwardFlag() && para.getWindwardRace()) {
            goWindwardMark();
        }
        if (para.getLeewardFlag() && para.getLeewardRace()) {
            goLeewardMark();
        }

        updateMarkerButtons();
        ScreenUpdate.post(updateScreenNow);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * changeActivityOnSwipe Method executes the desired action after the Class "swipeDetector"
     * detected a left or right swipe.  In this case we'll change to the MapsActivity on swipe detection
     */
    public void changeActivityOnSwipe() {
        // creating a bundle object to pass data to the Google Maps Fragment (MapsActivity)
        ScreenUpdate.removeCallbacks(updateScreenNow);   // stop the ScreenUpdate Handler Runnable
        para.setTWD(trueWindDirection);                  // save the current TWD
        startActivity(new Intent(appContext, MapsActivity.class));
    }

    /**
     * confirmQuit Method handles the "Quit" confirmation so that the user doesn't quit the race
     * activity by accidentially hitting the back button
     */
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

    /**
     * Runnable to update the display every "screenUpdates" seconds.  The screen update delay
     * can be specified using the Setting parameter Screen Updates.
     *
     * Within this runnable we obtain the latest GPS position, perform all computations, and
     * update the screen display with the results.
     */
    Runnable updateScreenNow = new Runnable() {
        public void run() {
        String tmp;
        double delta;
        double[] Laylines = new double[3];
        double[] goalPolars = new double[2];
        double goalVMG;

        if (gps.canGetLocation()) {
            para.setBoatLat(gps.getLatitude());
            para.setBoatLon(gps.getLongitude());
            gps.getHeading();
            gps.getSpeed();
            gps.getAvgSpeedHeading();
            counter = (counter + 1);
        } else {
            gps.showSettingsAlert();
        }

        // if we have an active Leeward or Windward mark, compute distance and Bearing to mark and the optimum Laylines
        if (para.getLeewardRace()) {
            DistanceBearing = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(), para.getLeewardLat(), para.getLeewardLon());
            Laylines = NavigationTools.optimumLaylines(para.getBoatLat(), para.getBoatLon(), para.getLeewardLat(), para.getLeewardLon(), trueWindDirection, para.getCourseOffset(), TackGybe, tack);
            para.setBestTack(NavigationTools.LaylinesString(Laylines[2]));
        }
        if (para.getWindwardRace()) {
            DistanceBearing = NavigationTools.MarkDistanceBearing(para.getBoatLat(), para.getBoatLon(), para.getWindwardLat(), para.getWindwardLon());
            Laylines = NavigationTools.optimumLaylines(para.getBoatLat(), para.getBoatLon(), para.getWindwardLat(), para.getWindwardLon(), trueWindDirection, para.getCourseOffset(), TackGybe, tack);
            para.setBestTack(NavigationTools.LaylinesString(Laylines[2]));
        }

        if (para.getLeewardRace() || para.getWindwardRace()) {
            outputDTM.setText(appContext.getString(R.string.DF2, DistanceBearing[0]));     // update DTM (Distance-To-Mark) in nm
            tmp = appContext.getString(R.string.Degrees,NavigationTools.fixAngle(DistanceBearing[1]+para.getDeclination()));
            outputBTM.setText(tmp);  // update BTM (Bearing-To-Mark)
            // computing meanHeadingTarget which now is theoretical BTM plus TackGybe angle
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(DistanceBearing[1] - TackGybe + para.getDeclination());
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(DistanceBearing[1] + TackGybe + para.getDeclination());
            }
        }

        if (windex == 1) {
            // dummy wind data
            trueWindDirection  = NavigationTools.fixAngle(trueWindDirection + TWAinc);
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

            goalPolars = NavigationTools.getPolars(trueWindSpeed, CourseOffset);
            goalVMG = goalPolars[0] * Math.cos(Math.toRadians(goalPolars[1]));

            TWSfifo.add(trueWindSpeed);
            TWAfifo.add(trueWindAngle);
            TWDfifo.add(trueWindDirection);
            //Log.d(LOG_TAG, "avg TWS: " + appContext.getString(R.string.DF1, TWSfifo.average()));
            //Log.d(LOG_TAG, "avg TWA: "+ appContext.getString(R.string.DF1, TWAfifo.average()));

            // end of dummy wind data

            if (para.getWindwardRace() || para.getWindwardRace()) {
                delta = NavigationTools.HeadingDelta(DistanceBearing[1], (trueWindDirection - CourseOffset));
            } else {
                delta = trueWindAngle-TackGybe;
            }
            tmp = appContext.getString(R.string.Degrees, NavigationTools.fixAngle(trueWindDirection + para.getDeclination()));
            outputTWD.setText(tmp);
            tmp = appContext.getString(R.string.Degrees, NavigationTools.fixAngle(trueWindAngle + para.getDeclination()));
            outputTWA.setText(tmp);
            tmp = appContext.getString(R.string.Degrees, NavigationTools.fixAngle(apparentWindAngle + para.getDeclination()));
            outputAWA.setText(tmp);
            outputTWS.setText(appContext.getString(R.string.DF1, trueWindSpeed));

            outputGoalVMG.setText(appContext.getString(R.string.DF1, goalVMG));
            tmp = para.getBestTack();
            if (tmp.equals("Stbd")) {
                outputGoalVMG.setTextColor(GREEN);
                outputGoalVMGtext.setTextColor(GREEN);
            } else if (tmp.equals("Port")) {
                outputGoalVMG.setTextColor(RED);
                outputGoalVMGtext.setTextColor(RED);
            } else {
                outputGoalVMG.setTextColor(WHITE);
                outputGoalVMGtext.setTextColor(WHITE);
            }
        } else {
            // compute the deviation from our "meanHeadingTarget"
            delta = NavigationTools.HeadingDelta(meanHeadingTarget, para.getCOG());
            outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
            tmp = appContext.getString(R.string.Degrees, NavigationTools.fixAngle(meanWindDirection + para.getDeclination()));
            outputMWD.setText(tmp);
        }

        updateBarChart(delta);
        lastAvgVariance = updateVarianceSumAvg(delta, lastAvgVariance);

        COGfifo.add(para.getCOG());
        SOGfifo.add(para.getSOG());

        /*
        add tack/gybe and course change detection after everything else
        has been debugged.
        checkForTackGybe();
        checkForUpwindDownwindChange();
        */

        tmp = appContext.getString(R.string.Degrees, para.getCOG());
        outputCOG.setText(tmp);

        tmp = appContext.getString(R.string.DF1, para.getSOG());
        outputSOG.setText(tmp); // or use avgSOG
        double vmgu = NavigationTools.calcVMGu(TWAfifo.average(), CourseOffset, para.getSOG());
        tmp = appContext.getString(R.string.DF1, vmgu);
        outputVMG.setText(tmp);

        outputStatus.setText(para.getGpsStatus());

        tmp = "Lat:" + NavigationTools.PositionDegreeToString(para.getBoatLat(), true);
        outputLatitude.setText(tmp);

        tmp = "Lon:" + NavigationTools.PositionDegreeToString(para.getBoatLon(), false) + "  δ:" + appContext.getString(R.string.DF1, para.getDeclination());
        outputLongitude.setText(tmp);

        // set the screen display frequency
        ScreenUpdate.postDelayed(this, (long) (screenUpdates * 1000));
        }
    };

    /**
     * Class methods to compute for the heading variances the sum totals and averages
     */
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
        if (CourseOffset == 0.0d) {
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
        tmp = appContext.getString(R.string.Degrees, Math.abs(avgVariance));
        outputAvgVariance.setText(tmp);
        tmp = appContext.getString(R.string.MinSec, (timeCounter / 60.0), NavigationTools.Mod(timeCounter, 60.0));
        outputVarDuration.setText(tmp);

        return avgVariance;
    }
    /**
     * Class methods to update the bar chart on bottom of screen displaying the heading or lift
     * degrees ranging from 0-20.
     */
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
                left = "left"+ appContext.getString(R.string.DFtwo, index);
                right = "right00";
            } else {
                left = "left00";
                right = "right" + appContext.getString(R.string.DFtwo, index);
            }
        } else {
            if (delta < 0.0) {
                left = "left"+appContext.getString(R.string.DFtwo, index);
                right = "right00";
            } else {
                left = "left00";
                right = "right"+appContext.getString(R.string.DFtwo, index);
            }
        }

        int leftImage = getResources().getIdentifier(left, "drawable", getPackageName());
        int rightImage = getResources().getIdentifier(right, "drawable", getPackageName());

        leftBar.setImageResource(leftImage);
        rightBar.setImageResource(rightImage);
    }

    /**
     * Class methods to increase/decrease the Mean Heading target values.
     * These Methods get initiated by UI buttons
     */
    public void decreaseMeanHeading(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 1.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 1.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void decreaseMeanHeading10(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - 10.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading10(View view) {
        meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 10.0);
        outputMeanHeadingTarget.setText(appContext.getString(R.string.DF0, meanHeadingTarget));
        meanWindDirection = MWD();
    }

    /**
     * Class methods to set the Windward mark at the current boat location.
     * initiated by UI buttons defined in activity_start_race and the onClick listener in onCreate
     *
     * @param mapMarkerSet - boolean that is true when a position has been set. In that case this
     *                       method will delete the existing mark
     */
    public void setWindwardMark(boolean mapMarkerSet) {
        if (!mapMarkerSet) {
            para.setWindwardLat(gps.getLatitude());
            para.setWindwardLon(gps.getLongitude());
        }
        if (para.getWindwardFlag()) {
            // display a confirmation dialog to make sure user wants to delete the mark position
            alert.show();
        } else {
            para.setWindwardFlag(true);
        }
        updateMarkerButtons();
    }

    /**
     * Class methods to set the Leeward mark at the current boat location.
     * initiated by UI buttons defined in activity_start_race and the onClick listener in onCreate
     *
     */
    public void setLeewardMark() {
        para.setLeewardLat(gps.getLatitude());
        para.setLeewardLon(gps.getLongitude());
        if (para.getLeewardFlag()) {
            // display a confirmation dialog to make sure user wants to delete the mark position
            alert.show();
        } else {
            para.setLeewardFlag(true);
        }
        updateMarkerButtons();
    }

    /**
     * The "clearPositionMark" method is called from the AlertDialog "alert" created in the
     * onCreate method when user confirms the deletion of a current Windward or Leeward marker.
     */
    public void clearPositionMark() {
        if (para.getWindwardFlag()) {
            para.setWindwardFlag(false);
            para.setWindwardRace(false);
            para.setWindwardLat(Double.NaN);
            para.setWindwardLon(Double.NaN);
        }
        if (para.getLeewardFlag()) {
            para.setLeewardFlag(false);
            para.setLeewardRace(false);
            para.setLeewardLat(Double.NaN);
            para.setLeewardLon(Double.NaN);
        }
        updateMarkerButtons();
    }
    /**
     * Class method to start racing toward Windward mark
     * initiated by UI buttons defined in activity_start_race
     */
    public void goWindwardMark() {
        if (para.getWindwardFlag()) {
            para.setWindwardRace(true);
            para.setLeewardRace(false);
            CourseOffset = 0.0d;            // on Upwind leg we sail to the wind, no correct required
            para.setCourseOffset(CourseOffset);
            TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, trueWindSpeed);
            para.setTackGybe(TackGybe);
        } else {
            para.setWindwardRace(false);
        }
        updateMarkerButtons();
    }

    /**
     * Class method to start racing toward Leeward mark
     * initiated by UI buttons defined in activity_start_race
     */
    public void goLeewardMark() {
        if (para.getLeewardFlag()) {
            para.setLeewardRace(true);
            para.setWindwardRace(false);
            CourseOffset = 180.0d;          // on Downwind leg we sail away from the wind
            para.setCourseOffset(CourseOffset);
            TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, trueWindSpeed);
            para.setTackGybe(TackGybe);
        } else {
            para.setLeewardRace(false);
        }
        updateMarkerButtons();
    }

    /**
     * Class method to update the color/text in the Marker Buttons
     */
    public void updateMarkerButtons() {
        if (windex == 0) {
            // only need to worry about these button when we're in the non-Windex mode
            if (para.getWindwardFlag() || para.getLeewardFlag()) {
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
            if (para.getLeewardFlag() && para.getLeewardRace()) {
                btnSetLeewardMark.setText("CLR");
                btnGoLeewardMark.setTextColor(GREEN);
            } else if (para.getLeewardFlag()) {
                btnSetLeewardMark.setText("CLR");
                btnGoLeewardMark.setTextColor(RED);
            } else {
                btnSetLeewardMark.setText("SET");
                btnGoLeewardMark.setTextColor(WHITE);
            }
            if (para.getWindwardFlag() && para.getWindwardRace()) {
                btnSetWindwardMark.setText("CLR");
                btnGoWindwardMark.setTextColor(GREEN);
            } else if (para.getWindwardFlag()) {
                btnSetWindwardMark.setText("CLR");
                btnGoWindwardMark.setTextColor(RED);
            } else {
                btnSetWindwardMark.setText("SET");
                btnGoWindwardMark.setTextColor(WHITE);
            }
        } else {
            if (para.getLeewardFlag() && para.getLeewardRace()) {
                outputLeewardMark.setText("LWD Mark: GO");
                outputLeewardMark.setTextColor(GREEN);
            } else if (para.getLeewardFlag()) {
                outputLeewardMark.setText("LWD Mark: SET");
                outputLeewardMark.setTextColor(RED);
            } else {
                outputLeewardMark.setText("LWD Mark: Not Set");
                outputLeewardMark.setTextColor(WHITE);
            }
            if (para.getWindwardFlag() && para.getWindwardRace()) {
                outputWindwardMark.setText("WWD Mark: GO");
                outputWindwardMark.setTextColor(GREEN);
            } else if (para.getWindwardFlag()) {
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

        if (CourseOffset == 0.0d) {
            // Upwind leg - we need to change meanHeadingTaget from downwind to upwind leg
            TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, trueWindSpeed);
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - gybeAngle - 180.0 - tackAngle);
                boat.setImageResource(R.drawable.boatgreen);
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + gybeAngle + 180.0 + tackAngle);
                boat.setImageResource(R.drawable.boatred);
            }
        } else {
            // Downwind leg - we need to change meanHeadingTaget from upwind to downwind leg
            TackGybe = NavigationTools.getTackAngle(windex, polars, tack, tackAngle, gybeAngle, CourseOffset, trueWindSpeed);
            if (tack.equals("stbd")) {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + tackAngle + 180.0 + gybeAngle);
                boat.setImageResource(R.drawable.boatgreen_reach);
            } else {
                meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget - tackAngle - 180.0 - gybeAngle);
                boat.setImageResource(R.drawable.boatred_reach);
            }
        }
        para.setTackGybe(TackGybe);
    }

    /**
     * Class method to compute a theoretical MWD from the meanHeadingTarget and the
     * assumed optimum tack angle
     */
    public double MWD() {
        if (tack.equals("stbd")) {
            return (NavigationTools.fixAngle(meanHeadingTarget + TackGybe + CourseOffset));
        }
        return (NavigationTools.fixAngle(meanHeadingTarget - TackGybe + CourseOffset));
    }

    /**
     * Class method to detect if we tacked or gybed on current course
     */
    void checkForTackGybe() {
        if ((Math.abs(NavigationTools.HeadingDelta(meanHeadingTarget, para.getCOG())) > 50.0) && (Math.abs(NavigationTools.HeadingDelta(meanWindDirection, para.getCOG())) < 80.0)) {
            // this should indicate that we tacked / gybed
            //;
        }
    }

    /**
     * Class method to detect if we're on Upwind or Downwind leg of race
     */
    void checkForUpwindDownwindChange() {
        if (Math.abs(NavigationTools.HeadingDelta(meanWindDirection, para.getCOG())) > 80.0) {
            // this should indicate that we are now sailing Downwind
            CourseOffset = 0.0d;
            para.setCourseOffset(CourseOffset);
        } else {
            // this should indicate that we are now sailing Upwind
            CourseOffset = 180.0d;
            para.setCourseOffset(CourseOffset);
        }
        if (Math.abs(NavigationTools.HeadingDelta(meanHeadingTarget, para.getCOG())) > 80.0) {
            meanHeadingTarget = NavigationTools.fixAngle(meanHeadingTarget + 180.0);
        }
        if (para.getLeewardRace() || para.getLeewardRace()) {
            if (CourseOffset == 180.0d) {
                goWindwardMark();
            } else {
                goLeewardMark();
            }
        }
    }
}
