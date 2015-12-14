package com.example.volkerpetersen.sailingrace;
/**
 * Created by Volker Petersen on November 2015.
 * Activity to setup the start timer, run the start timer, and to set windward and Leeward positions.
 *
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
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
    private Context appContext;
    private Button btnLeewardMarkSet;
    private Button btnWindwardMarkSet;
    private GlobalParameters para;
    static final String LOG_TAG = start_timerActivity.class.getSimpleName();
    private GPSTracker gps;
    private ColorStateList WHITE;
    private ColorStateList RED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_timer);
        setTitle("RaceApp - Timer");
        appContext = getApplicationContext();

        // fetch the Shared Preferences from the class FetchPreferenceValues in file SailingRacePreferences
        // Shared Preferences key names are defined in SailingRacePreferences.OnCreate()
        seconds = 0;
        minutes = SailingRacePreferences.FetchPreferenceValue("key_StartSequence", appContext);
        warning = SailingRacePreferences.FetchPreferenceValue("key_Warning", appContext);
        ourClass = SailingRacePreferences.FetchPreferenceValue("key_RaceClass", appContext);
        int history = SailingRacePreferences.FetchPreferenceValue("key_history", appContext); // max number of location positions stored in LinkedList.
        long gpsUpdates = (long) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateTime", appContext); // Time Interval for GPS position updates in milliseconds
        float minDistance = (float) SailingRacePreferences.FetchPreferenceValue("key_GPSUpdateDistance", appContext); // min distance (m) between GPS updates

        halftime = (int)(minutes*60/2.0);
        countdown = (minutes*60 + seconds) * interval;
        ct = (TextView) findViewById(R.id.countdownString);
        classSequence = (TextView) findViewById(R.id.classSequence);
        view = this.findViewById(android.R.id.content);

        // initialize our Global Parameter class by calling the
        // Application class (see application tag in AndroidManifest.xml)
        para = (GlobalParameters) appContext;

        gps = new GPSTracker(appContext, gpsUpdates, minDistance, history);
        if (gps.canGetLocation()) {
            para.setBoatLat(gps.getLatitude());
            para.setBoatLon(gps.getLongitude());
        }

        // initialize the color variables (type ColorStateList)
        WHITE = ContextCompat.getColorStateList(appContext, R.color.WHITE);
        RED = ContextCompat.getColorStateList(appContext, R.color.RED);

        // Initialize the set Windward / Leeward Map Position buttons and register btn listeners
        btnLeewardMarkSet = (Button) findViewById(R.id.button_setLeeward);
        btnWindwardMarkSet = (Button) findViewById(R.id.button_setWindward);
        updateButtons();

        btnLeewardMarkSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            // if we have a Click, we want to go Set or Clear the Leeward mark
            if (para.getLeewardFlag()) {
                // clear the Leeward Mark
                para.setLeewardFlag(false);
                para.setLeewardLat(Double.NaN);
                para.setLeewardLon(Double.NaN);
            } else {
                // set the Windward Mark
                para.setLeewardFlag(true);
                para.setLeewardLat(gps.getLatitude());
                para.setLeewardLon(gps.getLongitude());
            }
            updateButtons();
            }
        });

        btnWindwardMarkSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            // if we have a Click, we want to go Set or Clear the Windward mark
            if (para.getWindwardFlag()) {
                // clear the Windward Mark
                para.setWindwardFlag(false);
                para.setWindwardLat(Double.NaN);
                para.setWindwardLon(Double.NaN);
            } else {
                // start the Google Maps Fragment (MapsActivity) to set the Windward Mark
                startActivity(new Intent(appContext, MapsActivity.class));
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
        talk=new TextToSpeech(appContext, new TextToSpeech.OnInitListener() {
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
        if (para.getWindwardLat() != Double.NaN) {
            para.setWindwardRace(true);
        }
        super.onResume();
        updateButtons();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "+++++++++++++++++++++++++++++ On Destroy ++++++++++++++++++++++++++++++++");
        Log.d(LOG_TAG, "Windward Lat  " + appContext.getString(R.string.DF3, para.getWindwardLat()));
        Log.d(LOG_TAG, "Windward Race " + para.getWindwardRace());
        Log.d(LOG_TAG, "Windward Flag " + para.getWindwardFlag());
        Log.d(LOG_TAG, "Leeward Lat   " + appContext.getString(R.string.DF3, para.getLeewardLat()));
        Log.d(LOG_TAG, "Boat Lat      " + appContext.getString(R.string.DF3, para.getBoatLat()));
        Log.d(LOG_TAG, "para          " + para);
        gps.stopUsingGPS();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            //countDownTimer.onFinish();
        }
        talk.shutdown();
        super.onDestroy();
        finish(); //quit activity
    }


    public void updateButtons() {
        if (para.getLeewardFlag()) {
            btnLeewardMarkSet.setText("LWD CLR");
            btnLeewardMarkSet.setTextColor(RED);
        } else {
            btnLeewardMarkSet.setText("LWD SET");
            btnLeewardMarkSet.setTextColor(WHITE);
        }
        if (para.getWindwardFlag()) {
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
            startActivity(new Intent(appContext, start_raceActivity.class));
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
