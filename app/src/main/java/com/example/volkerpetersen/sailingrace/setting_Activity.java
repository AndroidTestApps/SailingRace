package com.example.volkerpetersen.sailingrace;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.text.DecimalFormat;


public class setting_Activity extends AppCompatActivity {
    // use the default shared preference file
    private SharedPreferences app_preferences;
    private DecimalFormat dfOne = new DecimalFormat("#");
    private EditText inputOurClass;
    private EditText inputTimerMinutes;
    private EditText inputTimerSeconds;
    private EditText inputScreenUpdates;
    private EditText inputGPSUpdates;
    private EditText inputMinDistance;
    private EditText inputTackAngle;
    private EditText inputGybeAngle;
    private EditText inputHistory;

    // preference variables
    private int timerMinutes;
    private int timerSeconds;
    private int ourClass;
    private int screenUpdates;
    private int history;
    private long gpsUpdates;
    private float minDistance;
    private float tackAngle;
    private float gybeAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setTitle("Offline - Settings");

        // variable initializations from the shared Preferences file
        app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        timerMinutes = (Integer)app_preferences.getInt("timerMinutes", (Integer)3);
        timerSeconds = (Integer)app_preferences.getInt("timerSeconds", (Integer)0);
        ourClass = (Integer)app_preferences.getInt("ourClass", (Integer)4);
        screenUpdates = (int)app_preferences.getInt("screenUpdates", (Integer)2); // Time interval for screen update in seconds.
        gpsUpdates = (Long)app_preferences.getLong("gpsUpdates", 1000);    // Time Interval for GPS position updates in milliseconds
        minDistance = (float) app_preferences.getFloat("minDistance", (float) 5.0);   // min distance (m) between GPS updates
        tackAngle = (float) app_preferences.getFloat("tackAngle", (float) 40.0);   // tack angle
        gybeAngle = (float) app_preferences.getFloat("gybeAngle", (float) 30.0);   // gybe angle
        history = (Integer)app_preferences.getInt("history", (Integer) 30);

        inputOurClass = (EditText) findViewById(R.id.ourClass);
        inputTimerMinutes = (EditText) findViewById(R.id.timerMinutes);
        inputTimerSeconds = (EditText) findViewById(R.id.timerSeconds);
        inputScreenUpdates = (EditText) findViewById(R.id.screenUpdates);
        inputGPSUpdates = (EditText) findViewById(R.id.gpsUpdates);
        inputMinDistance = (EditText) findViewById(R.id.minDistance);
        inputTackAngle = (EditText) findViewById(R.id.tackAngle);
        inputGybeAngle = (EditText) findViewById(R.id.gybeAngle);
        inputHistory = (EditText) findViewById(R.id.history);

        inputOurClass.setText(dfOne.format(ourClass));
        inputTimerMinutes.setText(dfOne.format(timerMinutes));
        inputTimerSeconds.setText(dfOne.format(timerSeconds));
        inputScreenUpdates.setText(dfOne.format(screenUpdates));
        inputGPSUpdates.setText(dfOne.format(gpsUpdates));
        inputMinDistance.setText(dfOne.format(minDistance));
        inputTackAngle.setText(dfOne.format(tackAngle));
        inputGybeAngle.setText(dfOne.format(gybeAngle));
        inputHistory.setText(dfOne.format(history));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    // class to handle the Update button and save the App parameters to file
    public void updateSettings(View view) {
        timerMinutes = Integer.parseInt(inputTimerMinutes.getText().toString() );
        timerSeconds = Integer.parseInt(inputTimerSeconds.getText().toString() );
        ourClass = Integer.parseInt(inputOurClass.getText().toString() );
        screenUpdates = Integer.parseInt(inputScreenUpdates.getText().toString());
        gpsUpdates = (long)Integer.parseInt(inputGPSUpdates.getText().toString());
        minDistance = (float)Integer.parseInt(inputMinDistance.getText().toString());
        tackAngle = (float)Integer.parseInt(inputTackAngle.getText().toString());
        gybeAngle = (float)Integer.parseInt(inputGybeAngle.getText().toString());
        history = Integer.parseInt(inputHistory.getText().toString() );

        SharedPreferences.Editor preferencesEditor = app_preferences.edit();
        preferencesEditor = app_preferences.edit();
        preferencesEditor.putInt("timerMinutes", timerMinutes);
        preferencesEditor.putInt("timerSeconds", timerSeconds);
        preferencesEditor.putInt("ourClass", ourClass);
        preferencesEditor.putInt("history", history);
        preferencesEditor.putInt("screenUpdates", screenUpdates); // Time interval for screen update in seconds.
        preferencesEditor.putLong("gpsUpdates", gpsUpdates);    // Time Interval for GPS position updates in milliseconds
        preferencesEditor.putFloat("minDistance", minDistance);   // min distance (m) between GPS updates
        preferencesEditor.putFloat("tackAngle", tackAngle);   // tack angle
        preferencesEditor.putFloat("gybeAngle", gybeAngle);   // tack angle
        boolean ret = preferencesEditor.commit();
        /*
        if (ret) {
            Toast.makeText(getApplicationContext(), "Preferences Saved! "+dfOne.format(ourClass), Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(getApplicationContext(), "Failure!!!!", Toast.LENGTH_LONG).show();
        }
        */
        finish();
    }
}
