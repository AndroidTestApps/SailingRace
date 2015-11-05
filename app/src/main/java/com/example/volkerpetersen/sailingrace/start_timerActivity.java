package com.example.volkerpetersen.sailingrace;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
    private long countdown;
    private long interval = 1 * 1000;
    private int ourClass;
    private int currentClass = 0;
    private String classNames[] = {"S2", "J/24", "J/22", "Sonar", "Capri25", "Ensign", "MORC"};
    private DecimalFormat twoDigits = new DecimalFormat("00");
    private NumberPicker[] nps=new NumberPicker[3];
    private TextView ct;
    private TextView classSequence;
    private CountDownTimer countDownTimer = null;
    private Button startB;
    private ToneGenerator sound;
    private TextToSpeech talk;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences app_preferences;
        final int PREFERENCES_MODE_PRIVATE = 0;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_timer);
        setTitle("RaceApp - Timer");

        // variable initializations from the shared Preferences file
        app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        minutes = app_preferences.getInt("timerMinutes", (Integer)3);
        seconds = app_preferences.getInt("timerSeconds", (Integer)0);
        ourClass = app_preferences.getInt("ourClass", (Integer)4);

        countdown = (minutes*60 + seconds) * interval;
        ct = (TextView) findViewById(R.id.countdownString);
        classSequence = (TextView) findViewById(R.id.classSequence);
        view = this.findViewById(android.R.id.content);

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
        talk=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    talk.setLanguage(Locale.US);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            //countDownTimer.onFinish();
        }
        finish(); //quit activity
    }

    public void counter_update() {
        countdown = (minutes*60 + seconds) * interval;
        ct.setText(twoDigits.format(minutes) + ":" + twoDigits.format(seconds));
        classSequence.setText("Your class "+classNames[ourClass]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_timer, menu);
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
            case R.id.race_mode:
                startActivity(new Intent(this, start_raceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // class to handle the Race button and start the race activity
    public void start_raceActivity(View view) {
        startActivity(new Intent(this, start_raceActivity.class));
    }

    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            talk.speak("GO", TextToSpeech.QUEUE_FLUSH, null);
            ct.setText(twoDigits.format(0) + ":" + twoDigits.format(0));
            finish(); //quit activity
            start_raceActivity(view);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int remaining;

            remaining = (int)(millisUntilFinished / interval) % (int)(countdown / interval);
            if (remaining == 0) {
                sound.startTone(ToneGenerator.TONE_PROP_BEEP, 250); // 2nd param is duration in ms
                currentClass = currentClass + 1;
                classSequence.setText(classNames[currentClass] + " Start");
            }
            minutes = (int)(remaining / 60.0);
            seconds = (remaining - minutes * 60);
            ct.setText(twoDigits.format(minutes) + ":" + twoDigits.format(seconds));
            if (remaining < 61 && remaining > 29){
                if ((remaining % 10) == 0) {
                    //debug.setText(twoDigits.format(remaining));
                    talk.speak(Integer.toString(remaining), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
            if (remaining < 29 && remaining >= 10){
                if ((remaining % 5) == 0) {
                    //debug.setText(twoDigits.format(remaining));
                    talk.speak(Integer.toString(remaining), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
            if (remaining < 10) {
                    //debug.setText(twoDigits.format(remaining));
                    sound.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150); // 2nd param is duration in
            }
        }
    }
    public void countdown_start(View view) {
        if(!timerHasStarted) {
            countDownTimer = new MyCountDownTimer(countdown*(ourClass+1), interval);
            countDownTimer.start();
            ct.setText(twoDigits.format(minutes) + ":" + twoDigits.format(seconds));
            classSequence.setText(classNames[currentClass] + " Start");
            startB.setText("STOP");
            timerHasStarted = true;
        } else {
            countDownTimer.cancel();
            timerHasStarted = false;
            startB.setText("RESET");
        }
    }
}
