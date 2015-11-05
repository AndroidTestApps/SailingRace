package com.example.volkerpetersen.sailingrace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.LinkedList;

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
    private Button btnSetLeewardMark;
    private Button btnGoLeewardMark;
    private Button btnSetWindwardMark;
    private Button btnGoWindwardMark;
    private Button btnMinusTen;
    private Button btnMinusOne;
    private Button btnPlusTen;
    private Button btnPlusOne;
    private View test;
    private ImageView boat;
    private ImageView leftBar;
    private ImageView rightBar;
    private Handler ScreenUpdate = new Handler();
    private GPSTracker gps;                 // gps class object
    private double longitude;               // current Latitude
    private double latitude;                // current Longitude
    private double declination;             // magnetic declination
    private double windwardLAT;             // Lat of race course Windward mark
    private double windwardLON;             // Lon of race course Windward mark
    private boolean windwardSet = false;    // true if Windward location has been set
    private boolean windwardRace = false;   // true if we race to Windward location
    private double leewardLAT;              // Lat of race course Leeward mark
    private double leewardLON;              // Lon of race course Leeward mark
    private boolean leewardSet = false;     // true if Leeward location has been set
    private boolean leewardRace = false;    // true if we race to Leeward location
    private double COG = 0.0;               // Course Over Ground computed from GPS location data
    private double SOG = 0.0;               // Speed Over Ground computed from GPS location data
    private double avgCOG = 0.0;            // average COG from the past 3 location data sets
    private double avgSOG = 0.0;            // average COG from the past 3 location data sets
    private double meanHeadingTarget;       // input with the buttons on top row or computed from current active mark
    private double meanWindDirection;       // calculated by adding tackAngle / gybeAngle to COG
    private double timeCounter = 0.0;       // keeps duration of current header / lift sequence
    private double sumVariances = 0.0;      // sum of the heading variances while in a Header or Lift sequence
    private double lastAvgVariance = 0.0;   // keeps track of the last avg variance between meanHeadingTarget and COG
    private int history;                    // number of past screen update values kept in FIFO queue
    private int screenUpdates;              // screen update frequency in sec.  Set in preferences.
    private String tack = "stbd";           // "stbd" or "port" depending on current active board we're sailing on
    private float CourseOffset=(float)0.0;  // Upwind leg=0.0  |  Downwind leg=180.0
    private float TackGybe;                 // contains tackAngle Upwind and gybeAngle Downwind
    private float tackAngle;                // upwind leg tack angle set in preferences
    private float gybeAngle;                // downwind leg gybe angle set in preferences
    private int counter = 0;
    private DecimalFormat dfThree = new DecimalFormat("000");
    private DecimalFormat dfTwo = new DecimalFormat("00");
    private DecimalFormat dfOne = new DecimalFormat("#");
    private DecimalFormat df3 = new DecimalFormat("#0.000");
    private DecimalFormat df2 = new DecimalFormat("#0.00");
    private DecimalFormat df1 = new DecimalFormat("#0.0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences app_preferences;
        final int PREFERENCES_MODE_PRIVATE = 0;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_race);
        setTitle("SailingRace - Racing");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Sailing Race App msg:");
        alertDialog.setMessage("Test");
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // here you can add more functions
            }
        });

        // variable initializations from the shared Preferences file
        app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        screenUpdates = app_preferences.getInt("screenUpdates", (Integer) 2); // Time interval for screen update in seconds.
        history = app_preferences.getInt("history", (Integer) 30); // max number of location positions stored in LinkedList.
        long gpsUpdates = app_preferences.getLong("gpsUpdates", 500);    // Time Interval for GPS position updates in milliseconds
        float minDistance = app_preferences.getFloat("minDistance", (float) 5.0);   // min distance (m) between GPS updates
        tackAngle = app_preferences.getFloat("tackAngle", (float) 40.0);    // tack angle
        gybeAngle = app_preferences.getFloat("gybeAngle", (float) 26.0);   // gybe angle with negative sign to reflect opposition direction than closed hauled
        TackGybe = tackAngle;

        // fetch all the display elements from the xml file
        outputStatus = (TextView) findViewById(R.id.Status);
        outputLatitude = (TextView) findViewById(R.id.Latitude);
        outputLongitude = (TextView) findViewById(R.id.Longitude);
        outputCOG = (TextView) findViewById(R.id.COG);
        outputSOG = (TextView) findViewById(R.id.SOG);
        outputVMG = (TextView) findViewById(R.id.VMG);
        outputMWD = (TextView) findViewById(R.id.MWD);
        outputMeanHeadingTarget = (TextView) findViewById(R.id.MeanHeadingTarget);
        outputHeaderLift = (TextView) findViewById(R.id.HeaderLift);
        outputAvgVariance = (TextView) findViewById(R.id.avgVariance);
        outputVarDuration = (TextView) findViewById(R.id.varDuration);
        outputBTM = (TextView) findViewById(R.id.markBearing);
        outputDTM = (TextView) findViewById(R.id.markDistance);
        btnSetLeewardMark = (Button) findViewById(R.id.buttonSetLeewardMark);
        btnGoLeewardMark = (Button) findViewById(R.id.buttonGoLeewardMark);
        btnSetWindwardMark = (Button) findViewById(R.id.buttonSetWindwardMark);
        btnGoWindwardMark = (Button) findViewById(R.id.buttonGoWindwardMark);
        btnMinusTen = (Button) findViewById(R.id.buttonMinus10);
        btnMinusOne = (Button) findViewById(R.id.buttonMinus);
        btnPlusTen = (Button) findViewById(R.id.buttonPlus10);
        btnPlusOne = (Button) findViewById(R.id.buttonPlus);
        boat = (ImageView) findViewById(R.id.boat);
        leftBar = (ImageView) findViewById(R.id.leftBar);
        rightBar = (ImageView) findViewById(R.id.rightBar);

        // initialize the GPS tracker, ScreenUpdate and the Chart Plotter
        btnGoLeewardMark.setEnabled(false);
        btnGoWindwardMark.setEnabled(false);
        meanHeadingTarget = Integer.parseInt(outputMeanHeadingTarget.getText().toString() );;
        gps = new GPSTracker(start_raceActivity.this, gpsUpdates, minDistance, history);
        ScreenUpdate.post(updateScreenNow);

        boat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view == findViewById(R.id.boat)) {
                    readyToTackOrGybe();
                }
                outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
            }
        });

        boat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // if we have a long Click, we want to change from Upwind to Downwind or visa versa
                CourseOffset = (float)180.0 - CourseOffset;
                if (CourseOffset == 0.0) {
                    TackGybe = tackAngle;
                } else {
                    TackGybe = -gybeAngle;
                }
                updateCommonStuff(false);
                //alertDialog.setMessage("We detected a long click on the boat image!");
                //alertDialog.show();
                //Toast toast = Toast.makeText(getApplicationContext(), "long Click on Boat", Toast.LENGTH_LONG);
                //toast.setGravity(Gravity.TOP| Gravity.CENTER, 0, 0);
                //toast.show();
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gps.stopUsingGPS();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {

        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
       }

    @Override
    protected void onPause() {
        super.onPause();
        gps.stopUsingGPS();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                startActivity(new Intent(this, setting_Activity.class));
                return true;
            case R.id.timer_mode:
                startActivity(new Intent(this, start_timerActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //-----------------------------------------------------------------------------------------
    // Class method to update the display every "screenUpdates" seconds
    //-----------------------------------------------------------------------------------------
    Runnable updateScreenNow = new Runnable() {
        public void run() {
            double [] DistanceBearing = new double[2];
            if (gps.canGetLocation()) {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                gps.getHeading();
                gps.getSpeed();
                gps.getAvgSpeedHeading();
                counter = (counter + 1);
            } else {
                gps.showSettingsAlert();
            }

            // if we have an active Leeward or Windward mark, compute distance and Bearing to mark
            if (leewardRace) {
                DistanceBearing = MarkDistanceBearing(latitude, longitude, leewardLAT, leewardLON);
            }
            if (windwardRace) {
                DistanceBearing = MarkDistanceBearing(latitude, longitude, windwardLAT, windwardLON);
            }

            if (leewardRace || windwardRace) {
                outputDTM.setText(df2.format(DistanceBearing[0]));          // update DTM (Distance-To-Mark) in nm
                outputBTM.setText(dfOne.format(DistanceBearing[1]) + "°");  // update BTM (Bearing-To-Mark)
                // computing meanHeadingTarget which now is theoretical BTM plus TackGybe angle
                if (tack.equals("stbd")) {
                    meanHeadingTarget = fixAngle(DistanceBearing[1] - TackGybe);
                } else {
                    meanHeadingTarget = fixAngle(DistanceBearing[1] + TackGybe);
                }
            }

            /*
            add tack/gybe and course change detection after everything else
            has been debugged.
            checkForTackGybe();
            checkForUpwindDownwindChange();
            */

            // compute the deviation from our "meanHeadingTarget"
            double delta = HeadingDelta(COG - meanHeadingTarget);
            updateBarChart(delta);
            lastAvgVariance = updateVarianceSumAvg(delta, lastAvgVariance);

            outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
            outputLatitude.setText("Lat:" + PositionDegreeToString(latitude, true));
            outputLongitude.setText("Lon:" + PositionDegreeToString(longitude, false) + "  δ:" + df1.format(declination));
            outputCOG.setText(dfOne.format(COG) + "°");
            outputSOG.setText(df1.format(SOG)); // or use avgSOG
            outputMWD.setText(dfOne.format(meanWindDirection)+"°");
            double vmg = calcVMG(DistanceBearing[1], COG, SOG);
            if (vmg != -99.99) {
                outputVMG.setText(df1.format(vmg));
            } else {
                outputVMG.setText("- - ");
            }

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

        // calculate the correct sign for the course / board we are currently sailing on.
        // We want a Header be negative values and Lift positive values based on the
        // current course and delta = meanHeadingTarget-COG
        //
        //           stbd     port
        //           H   L    H   L
        //          (-) (+)  (-) (+)
        // Upwind    -   +    +   -    necessary value of sign in order to yield a negative values
        // Downwind  +   -    -   +    for Header and positive values for Lift situations
        //
        if (CourseOffset == 0.0) {
            // Upwind leg
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
        } else {
            outputHeaderLift.setText("Lift");
        }
        outputAvgVariance.setText(dfOne.format(Math.abs(avgVariance)) + "°");
        outputVarDuration.setText(dfTwo.format((long)(timeCounter/60.0))+":"+dfTwo.format(Mod(timeCounter, 60.0)));

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
        meanHeadingTarget = fixAngle(meanHeadingTarget - 1.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading(View view) {
        meanHeadingTarget = fixAngle(meanHeadingTarget + 1.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void decreaseMeanHeading10(View view) {
        meanHeadingTarget = fixAngle(meanHeadingTarget - 10.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    public void increaseMeanHeading10(View view) {
        meanHeadingTarget = fixAngle(meanHeadingTarget + 10.0);
        outputMeanHeadingTarget.setText(dfThree.format(meanHeadingTarget));
        meanWindDirection = MWD();
    }

    //-----------------------------------------------------------------------------------------
    // Class methods to set Windward and Leeward marks
    // initiated by UI buttons
    //-----------------------------------------------------------------------------------------
    public void setWindwardMark(View view) {
        windwardLAT = gps.getLatitude();
        windwardLON = gps.getLongitude();
        windwardSet = true;
        btnGoWindwardMark.setEnabled(true);
        btnSetWindwardMark.setTextColor(getResources().getColor(R.color.WHITE));
        btnGoWindwardMark.setTextColor(getResources().getColor(R.color.RED));
        CourseOffset = (float) 180.0;   // on Downwind leg we sail away from the wind
        TackGybe = -gybeAngle;          // since on Downwind leg the math is opposite to Upwind legs
        updateCommonStuff(false);
    }

    public void setLeewardMark(View view) {
        leewardLAT = gps.getLatitude();
        leewardLON = gps.getLongitude();
        leewardSet = true;
        btnGoLeewardMark.setEnabled(true);
        btnSetLeewardMark.setTextColor(getResources().getColor(R.color.WHITE));
        btnGoLeewardMark.setTextColor(getResources().getColor(R.color.RED));
        goWindwardMark(view);
    }

    //-----------------------------------------------------------------------------------------
    // Class methods to start racing toward Windward and Leeward marks
    // initiated by UI buttons
    //-----------------------------------------------------------------------------------------
    public void goWindwardMark (View view) {
        if (windwardSet) {
            btnGoWindwardMark.setTextColor(getResources().getColor(R.color.GREEN));
            windwardRace = true;
            leewardRace = false;
            CourseOffset = (float) 0.0;     // on Upwind leg we sail to the wind, no correct required
            TackGybe = tackAngle;
            updateCommonStuff(true);
        } else {
            windwardRace = false;
        }
        if (leewardSet) {
            btnGoLeewardMark.setTextColor(getResources().getColor(R.color.RED));
        } else {
            btnGoLeewardMark.setTextColor(getResources().getColor(R.color.WHITE));
        }
    }

    public void goLeewardMark (View view) {
        if (leewardSet) {
            btnGoLeewardMark.setTextColor(getResources().getColor(R.color.GREEN));
            leewardRace = true;
            windwardRace = false;
            CourseOffset = (float) 180.0;   // on Downwind leg we sail away from the wind
            updateCommonStuff(true);
        } else {
            leewardRace = false;
        }
        if (windwardSet) {
            btnGoWindwardMark.setTextColor(getResources().getColor(R.color.RED));
        } else {
            btnGoWindwardMark.setTextColor(getResources().getColor(R.color.WHITE));
        }
    }

    public void updateCommonStuff(boolean disableBtn) {
        // we only update the button status when the "disableBtn" is set to true
        if (disableBtn) {
            btnMinusTen.setEnabled(false);
            btnMinusOne.setEnabled(false);
            btnPlusTen.setEnabled(false);
            btnPlusOne.setEnabled(false);
        }
        timeCounter = 0.0;
        sumVariances = 0.0;
        gps.LocationList.clear();
        gps.LocationList.clear();
        meanHeadingTarget = fixAngle(meanHeadingTarget + 180.0);
        if (CourseOffset == 0.0) {
            // Upwind leg
            TackGybe = tackAngle;
            if (tack.equals("stbd")) {
                boat.setImageResource(R.drawable.boatgreen);
            } else {
                boat.setImageResource(R.drawable.boatred);
            }
        } else {
            // Downwind leg
            TackGybe = -gybeAngle;          // since on Downwind leg the math is opposite to Upwind legs
            if (tack.equals("stbd")) {
                boat.setImageResource(R.drawable.boatgreen_reach);
            } else {
                boat.setImageResource(R.drawable.boatred_reach);
            }
        }
    }

    public double fixAngle(double angle) {
        return Mod(angle, 360.0);
        /*
        if (angle >= 360.0) {
            return (angle - 360.0);
        }
        if (angle < 0.0) {
            return (360.0 + angle);
        }
        return angle;
        */
    }
    //-----------------------------------------------------------------------------------------
    // Class method to compute a theoretical MWD from the meanHeadingTarget and the
    // assumed optimum tack angle
     //-----------------------------------------------------------------------------------------
    public double MWD() {
        if (tack.equals("stbd")) {
            return (fixAngle(meanHeadingTarget + TackGybe + CourseOffset));
        }
        return (fixAngle(meanHeadingTarget - TackGybe + CourseOffset));
    }

    //-----------------------------------------------------------------------------------------
    // Class method to detect if we tacked or gybed on current course
    //-----------------------------------------------------------------------------------------
    void checkForTackGybe() {
        if ((Math.abs(HeadingDelta(meanHeadingTarget - COG)) > 50.0) && (Math.abs(HeadingDelta(meanWindDirection - COG)) < 80.0)) {
            // this should indicate that we tacked / gybed
            readyToTackOrGybe();
        }
    }

    //-----------------------------------------------------------------------------------------
    // Class method to detect if we're on Upwind or Downwind leg of race
    //-----------------------------------------------------------------------------------------
    void checkForUpwindDownwindChange() {
        if (Math.abs(HeadingDelta(meanWindDirection - COG)) > 80.0) {
            // this should indicate that we are now sailing Downwind
            CourseOffset = (float) 0.0;
        } else {
            // this should indicate that we are now sailing Upwind
            CourseOffset = (float) 180.0;
        }
        if (Math.abs(HeadingDelta(meanHeadingTarget - COG)) > 80.0) {
            meanHeadingTarget = fixAngle(meanHeadingTarget + 180.0);
        }
        if (leewardRace || windwardRace) {
            if (CourseOffset == 180.0) {
                goWindwardMark(test);
            } else {
                goLeewardMark(test);
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
            meanHeadingTarget = fixAngle(meanHeadingTarget + 2.0*TackGybe);
            outputMeanHeadingTarget.setTextColor(getResources().getColor(R.color.RED));
        } else {
            tack = "stbd";
            if (CourseOffset == 0.0) {
                // Upwind leg
                boat.setImageResource(R.drawable.boatgreen);
            } else {
                // Downwind leg
                boat.setImageResource(R.drawable.boatgreen_reach);
            }
            meanHeadingTarget = fixAngle(meanHeadingTarget - 2.0*TackGybe);
            outputMeanHeadingTarget.setTextColor(getResources().getColor(R.color.GREEN));
        }
        timeCounter = 0.0;
        sumVariances = 0.0;
        gps.LocationList.clear();
    }
    //-----------------------------------------------------------------------------------------
    // Class method to compute the Velocity Made Good (VMG) toward the current active mark.
    // Inputs:
    //  (double) btm - Bearing-To-Mark
    //  (double) cog - Course-Over-Ground (both assumed to be magnetic)
    //  (double) sog - Speed-Over-Ground
    // returns double vmg
    //-----------------------------------------------------------------------------------------
    protected double calcVMG(double btm, double cog, double sog) {
        double vmg = (double) -99.99;
        double offcourse;

        offcourse = HeadingDelta(btm - cog);
        if (offcourse >= -90.0 && offcourse <= 90.0) {
            vmg = Math.cos(toRad(offcourse)) * sog;
        }
        return vmg;
    }

    //-----------------------------------------------------------------------------------------
    // Class method to compute Distance (in nm) and Bearing (in degrees) to current active mark
    // using the Great Circle calculations.
    // returns array values = [Distance, Bearing]
    //-----------------------------------------------------------------------------------------
    protected double [] MarkDistanceBearing(double latFrom, double lonFrom, double latTo, double lonTo) {
        double km_nm = 0.539706;        /* km to nm conversion factor */
        double Radius = 6371.0*km_nm;	/* Earth Radius in nm */

        double [] values = new double[2];
        double dLat = toRad(latTo-latFrom);
        double dLon = toRad(lonTo-lonFrom);
        double lat1 = toRad(latFrom);
        double lat2 = toRad(latTo);

        double a = Math.sin(dLat / 2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        values[0] = Radius * c;          			// distance
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        values[1] = toDegree(Math.atan2(y, x));             // initial bearing
        values[1] = Mod(values[1]+declination, 360.0);      // final bearing adjusted to compass heading
        return values;
    }

    protected double toRad(double Value) {
        /** Converts numeric degrees to radians */
        return Value * Math.PI / 180.0;
    }

    protected double toDegree(double Value) {
        /** Converts numeric radians to degrees */
        return Value * 180.0 / Math.PI;
    }

    protected double Mod(double a, double b) {
        /** Modulo function */
        while (a < 0) {
            a += b;
        }
        return a % b;

    }

    //-----------------------------------------------------------------------------------------
    // Class method to convert a double position into a degree and decimal minutes string
    //-----------------------------------------------------------------------------------------
    protected String PositionDegreeToString(double wp, boolean flag) {
        double wpabs = Math.abs(wp);
        int d = (int) (wpabs);
        double m = (wpabs - d) * 60.0;
        String strg = df3.format(m);
        if (flag) {        // we convert a latitude
            if (wp > 0) {
                strg = dfTwo.format(d) + "° " + strg + "'' N";
            } else {
                strg = dfTwo.format(d) + "° " + strg + "'' S";
            }
        } else {              // we convert a longitude
            if (wp > 0) {
                strg = dfThree.format(d) + "° " + strg + "'' E";
            } else {
                strg = dfThree.format(d) + "° " + strg + "'' W";
            }
        }
        return (strg);
    }

    public Double HeadingDelta(double delta) {
        if (delta > 180.0) {
            delta = delta - 360.0;
        }
        if (delta < -180.0) {
            delta = -delta - 360.0;
        }
        return delta;
    }

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
        private boolean isGPSEnabled = false;
        private boolean canGetLocation = false;
        private int GPSfired;
        private int keepNumPositions;
        private double toKts = 1.94384;  // m/s to knots conversation factor
        private Location location;
        private LocationManager locationManager;
        private LinkedList<Location> LocationList = new LinkedList<Location>();
        private String[] dots;

        public GPSTracker(Context context, long gpsUpdates, float gpsDistance, int keepNumPositions) {
            this.context = context;
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
                declination = geoField.getDeclination();
            } else {
                declination = 0.0;
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
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minGPSUpdateTime, minGPSDistance, this);

                    if (locationManager != null) {
                        loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        if (loc != null) {
                            this.canGetLocation = true;
                            latitude = gps.getLatitude();
                            longitude = gps.getLongitude();
                            gps.getHeading();
                            gps.getSpeed();
                            gps.getAvgSpeedHeading();
                            outputMeanHeadingTarget.setText(dfThree.format(avgCOG));
                            meanHeadingTarget = avgCOG;
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
                locationManager.removeUpdates(GPSTracker.this);
            }
        }

        public double getLatitude() {
            if (location != null) latitude = location.getLatitude();
            return latitude;
        }

        public double getLongitude() {
            if (location != null) longitude = location.getLongitude();
            return longitude;
        }

        public void getSpeed() {
            if (location != null) SOG = location.getSpeed() * toKts;
        }

        public void getHeading() {
            // compute current heading of the boat as average of the current GPS heading and
            // the heading over the past 3 datapoints.
            if (location != null) {
                COG = location.getBearing() + declination;
                int index = LocationList.size() - 3;  // get index of 2 "position fixes" prior to last "position fix"
                /*
                if (index >= 0) {
                    COG = fixAngle((COG + LocationList.get(index).bearingTo(location) + declination) / 2.0);
                }
                */
            }
        }

        public void getAvgSpeedHeading() {
            if (location != null && LocationList.size()>2 ) {
                Location first = LocationList.getFirst();
                Location last = LocationList.getLast();

                avgCOG = fixAngle(first.bearingTo(last) + declination);
                double distance = first.distanceTo(last);
                double time = (last.getElapsedRealtimeNanos() - first.getElapsedRealtimeNanos()) / 1000000000.0;
                if (time != 0.0) {
                    avgSOG = distance / time * toKts;
                }
            } else {
                avgCOG = COG;
                avgSOG = SOG;
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
            outputStatus.setText(provider + dots[GPSfired%4]);
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
}
