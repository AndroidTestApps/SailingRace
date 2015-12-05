package com.example.volkerpetersen.sailingrace;
/**
 * Created by Volker Petersen on November 2015.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.Locale;

public class start_timerActivity extends Activity {
    private boolean timerHasStarted = false;
    private int minutes;
    private int seconds;
    private int countdown;
    private int halftime;
    private int interval = 1 * 1000;
    private int ourClass;
    private int currentClass = 0;
    private int warning = 1;  // warning = 1 if a warning period is at the beginning of the the start sequence
    private String classNames[] = {"S2", "J/24", "J/22", "Sonar", "Capri25", "Ensign", "MORC"};
    private DecimalFormat twoDigits = new DecimalFormat("00");
    private NumberPicker[] nps=new NumberPicker[3];
    private TextView ct;
    private View view;
    private TextView classSequence;
    private CountDownTimer countDownTimer = null;
    private Button startB;
    private ToneGenerator sound;
    private TextToSpeech talk;
    private Context context;
    private Button btnLeewardMarkSet;
    private Button btnWindwardMarkSet;
    private GlobalParameters para;
    static final String LOG_TAG = start_timerActivity.class.getSimpleName();
    static final int GET_MAP_MARKER_POSITION = 1;   // Our request code to pass data back from map FragmentActivity
    private DecimalFormat df2 = new DecimalFormat("#0.00");
    private DecimalFormat df1 = new DecimalFormat("#0.0");
    private GPSTracker gps;
    private ColorStateList WHITE;
    private ColorStateList RED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_timer);
        setTitle("RaceApp - Timer");
        context = getApplicationContext();

        // fetch the Shared Preferences from the class FetchPreferenceValues in file SailingRacePreferences
        // Shared Preferences key names are defined in SailingRacePreferences.OnCreate()
        seconds = 0;
        minutes = SailingRacePreferences.FetchPreferenceValue("key_StartSequence", getBaseContext());
        warning = SailingRacePreferences.FetchPreferenceValue("key_Warning", getBaseContext());
        ourClass = SailingRacePreferences.FetchPreferenceValue("key_RaceClass", getBaseContext());
        int history = SailingRacePreferences.FetchPreferenceValue("key_history", context); // max number of location positions stored in LinkedList.
        long gpsUpdates = (long) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateTime", context); // Time Interval for GPS position updates in milliseconds
        float minDistance = (float) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateDistance", context); // min distance (m) between GPS updates

        halftime = (int)(minutes*60/2.0);
        countdown = (minutes*60 + seconds) * interval;
        ct = (TextView) findViewById(R.id.countdownString);
        classSequence = (TextView) findViewById(R.id.classSequence);
        view = this.findViewById(android.R.id.content);

        // initialize our Global Parameter class and the gps location class
        para = new GlobalParameters();
        //Log.d(LOG_TAG, "start_timerActivity value of 'TEST': "+para.test);
        //para.test = "leaving start_timerActivity";

        gps = new GPSTracker(start_timerActivity.this, para, gpsUpdates, minDistance, history);
        if (gps.canGetLocation()) {
            para.latitude = gps.getLatitude();
            para.longitude = gps.getLongitude();
        }

        // initialize the color variables (type ColorStateList)
        WHITE = ContextCompat.getColorStateList(context, R.color.WHITE);
        RED = ContextCompat.getColorStateList(context, R.color.RED);

        // Initialize the set Windward / Leeward Map Position buttons and register btn listeners
        btnLeewardMarkSet = (Button) findViewById(R.id.button_setLeeward);
        btnWindwardMarkSet = (Button) findViewById(R.id.button_setWindward);
        updateButtons();

        btnLeewardMarkSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we have a Click, we want to go Set or Clear the Leeward mark
                if (para.leewardSet) {
                    para.leewardSet = false;
                    para.leewardLAT = Double.NaN;
                    para.leewardLON = Double.NaN;
                } else {
                    para.leewardSet = true;
                    para.leewardLAT = para.latitude;
                    para.leewardLON = para.longitude;
                }
                updateButtons();
            }
        });

        btnWindwardMarkSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we have a Click, we want to go Set or Clear the Windward mark
                if (para.windwardSet) {
                    para.windwardSet = false;
                    para.windwardLAT = Double.NaN;
                    para.windwardLON = Double.NaN;
                } else {
                    // creating a bundle object to pass data to the Google Maps Fragment (MapsActivity)
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("windwardRace", para.windwardRace);
                    bundle.putBoolean("leewardRace", para.leewardRace);
                    bundle.putDouble("windwardLat", para.windwardLAT);
                    bundle.putDouble("windwardLon", para.windwardLON);
                    bundle.putDouble("leewardLat", para.leewardLAT);
                    bundle.putDouble("leewardLon", para.leewardLON);
                    bundle.putDouble("boatLat", para.latitude);
                    bundle.putDouble("boatLon", para.longitude);

                    Intent intent = new Intent(context, MapsActivity.class);
                    intent.putExtras(bundle);
                    // the "startActivityForResults" system method allows the calls upon Fragment to return results.
                    // the results are processed in the method onActivityResult() below.
                    startActivityForResult(intent, GET_MAP_MARKER_POSITION);
                }
                updateButtons();
            }
        });

        // Initialize the three Number Pickes
        startB = (Button) findViewById(R.id.button_countdownStart);
        nps[0]= (NumberPicker) findViewById(R.id.numberPickerMinutes);
        nps[1]= (NumberPicker) findViewById(R.id.numberPickerSeconds);
        nps[2]= (NumberPicker) findViewById(R.id.numberPickerClass);

        NumberPicker.OnValueChangeListener onValueChangedMinutes=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                minutes = newVal;
                counter_update();
            }
        };
        NumberPicker.OnValueChangeListener onValueChangedSeconds=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                seconds = newVal;
                counter_update();
            }
        };
        NumberPicker.OnValueChangeListener onValueChangedClass=new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                ourClass = newVal;
                counter_update();
            }
        };

        String[] values = new String[60];
        for (int i = 0; i < values.length; i++){
            values[i]=Integer.toString(i);
        }

        for(int i=0;i<3;i++){
            nps[i].setMinValue(0);
            if (i == 2) {
                nps[i].setMaxValue(classNames.length - 1);
                nps[i].setDisplayedValues(classNames);
            } else {
                nps[i].setMaxValue(values.length - 1);
                nps[i].setDisplayedValues(values);
            }
        }
        nps[0].setOnValueChangedListener(onValueChangedMinutes);
        nps[1].setOnValueChangedListener(onValueChangedSeconds);
        nps[2].setOnValueChangedListener(onValueChangedClass);

        nps[0].setValue(minutes);
        nps[1].setValue(seconds);
        nps[2].setValue(ourClass);
        nps[2].setWrapSelectorWheel(false);
        counter_update();

        // initialize the Tome Generator
        sound = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        // initialize the Text-to-Speech app
        talk=new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    talk.setLanguage(Locale.US);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        gps.stopUsingGPS();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            //countDownTimer.onFinish();
        }
        talk.shutdown();
        super.onDestroy();
        finish(); //quit activity
    }

    // method that retrieves the data which has been returned from the Google Maps FragmentActivity (MapsActivity)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        double[] DistanceBearing = new double[2];
        super.onActivityResult(requestCode, resultCode, intent);
        // Check which request we're responding to.  Here we're interested in the Google Maps marker position
        if (requestCode == GET_MAP_MARKER_POSITION) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // a Marker position has been set.  Make that our Windward mark.
                Bundle extras = intent.getExtras();
                para.windwardSet = true;
                para.windwardLAT = extras.getDouble("markerLat");
                para.windwardLON = extras.getDouble("markerLon");
                updateButtons();
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

    public void updateButtons() {
        if (para.leewardSet) {
            btnLeewardMarkSet.setText("LWD CLR");
            btnLeewardMarkSet.setTextColor(RED);
        } else {
            btnLeewardMarkSet.setText("LWD SET");
            btnLeewardMarkSet.setTextColor(WHITE);
        }
        if (para.windwardSet) {
            btnWindwardMarkSet.setText("WWD CLR");
            btnWindwardMarkSet.setTextColor(RED);
        } else {
            btnWindwardMarkSet.setText("WWD SET");
            btnWindwardMarkSet.setTextColor(WHITE);
        }
    }
    public void counter_update() {
        String tmp;
        countdown = (minutes*60 + seconds) * interval;
        tmp = twoDigits.format(minutes) + ":" + twoDigits.format(seconds);
        ct.setText(tmp);
        tmp = "Your class "+classNames[ourClass];
        classSequence.setText(tmp);
    }


    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            String tmp = twoDigits.format(0) + ":" + twoDigits.format(0);
            CharSequence charSeq = "Go";
            ct.setText(tmp);
            talk.speak(charSeq, TextToSpeech.QUEUE_FLUSH, null, "go");
            startActivity(new Intent(context, start_raceActivity.class));
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int remaining;
            String tmp;
            CharSequence charSeq;

            remaining = (int)(millisUntilFinished / interval) % (int)(countdown / interval);
            if (remaining == 0) {
                sound.startTone(ToneGenerator.TONE_PROP_BEEP, 250); // 2nd param is duration in ms
                currentClass = currentClass + 1;
                classSequence.setText(classNames[currentClass-warning] + " Start");
            }
            minutes = (int)(remaining / 60.0);
            seconds = (remaining - minutes * 60);
            tmp = twoDigits.format(minutes) + ":" + twoDigits.format(seconds);
            ct.setText(tmp);
            if (remaining < 61 && remaining > 29){
                if ((remaining % 10) == 0) {
                    //debug.setText(twoDigits.format(remaining));
                    tmp = Integer.toString(remaining);
                    charSeq = (CharSequence)tmp;
                    talk.speak(charSeq, TextToSpeech.QUEUE_FLUSH, null, tmp);
                }
            }
            if (remaining < 29 && remaining >= 10){
                if ((remaining % 5) == 0) {
                    //debug.setText(twoDigits.format(remaining));
                    tmp = Integer.toString(remaining);
                    charSeq = (CharSequence)tmp;
                    talk.speak(charSeq, TextToSpeech.QUEUE_FLUSH, null, tmp);
                }
            }
            if (remaining < 10) {
                    //debug.setText(twoDigits.format(remaining));
                    sound.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150); // 2nd param is duration in
            }
        }
    }
    public void countdown_start(View view) {
        String tmp;
        // method is called when clicking on Button "button_countdownStart" defined in "activity_start_timer.xml"
        // this button is cycled thru this sequence: Start->Stop->Reset->Start.
        // if Reset is pressed with more than 50% left in current countdown, reset and start from top in current class
        // if Reset is pressed with less than 50% left in current countdown, reset and start from top in next class
        // current class is being counted up from 0 to < ourClass
        // ourClass contains the class number of our race class
        if(!timerHasStarted) {
            countDownTimer = new MyCountDownTimer(countdown*(ourClass+warning+1-currentClass), interval);
            countDownTimer.start();
            tmp = twoDigits.format(minutes) + ":" + twoDigits.format(seconds);
            ct.setText(tmp);
            if (currentClass == 0 && warning == 1) {
                tmp = "Warning Period";
            } else {
                tmp = classNames[currentClass-warning] + " Start";
            }
            classSequence.setText(tmp);
            startB.setText("STOP");
            timerHasStarted = true;
            nps[0].setEnabled(false);
            nps[1].setEnabled(false);
            nps[2].setEnabled(false);
        } else {
            countDownTimer.cancel();
            timerHasStarted = false;
            startB.setText("RESET");
            /*
            Log.d(LOG_TAG, "Minutes: " + twoDigits.format(minutes));
            Log.d(LOG_TAG, "Seconds: "+twoDigits.format(seconds));
            Log.d(LOG_TAG, "Our Class: " + twoDigits.format(ourClass));
            Log.d(LOG_TAG, "Current Class: "+twoDigits.format(currentClass));
            Log.d(LOG_TAG, "Warning: " + twoDigits.format(warning));
            */

            if (minutes * 60 + seconds < halftime) {
                currentClass += 1;
            }
            nps[0].setEnabled(true);
            nps[1].setEnabled(true);
            nps[2].setEnabled(true);
        }
    }
}
