package com.example.volkerpetersen.sailingrace;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;

public class MainActivity extends Activity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public Context context;
    public GlobalParameters para;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Offline");
        context = getApplicationContext();
        para = new GlobalParameters();
        para.windwardRace = false;
        para.windwardSet = false;
        para.windwardLAT = Double.NaN;
        para.windwardLON = Double.NaN;
        para.leewardRace = false;
        para.leewardSet = false;
        para.leewardLAT = Double.NaN;
        para.leewardLON = Double.NaN;
        //Log.d(LOG_TAG, "initial setting of 'TEST': "+para.test);
        //para.test = "leaving MainActivity";
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(context, SailingRacePreferences.class));
                return true;
            case R.id.race_mode:
                startActivity(new Intent(context, start_raceActivity.class));
                return true;
            case R.id.timer_mode:
                startActivity(new Intent(context, start_timerActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // class to handle the Timer button to start the Timer activity
    public void start_timerActivity(View view) {
        startActivity(new Intent(context, start_timerActivity.class));
    }

    // class to handle the Race button and start the race activity
    public void start_raceActivity(View view) {
        startActivity(new Intent(context, start_raceActivity.class));
    }

    // class to handle the Settings button to allow user to customize the program parameters
    public void start_settingsActivity(View view) {
        //Log.e(LOG_TAG, "Reached the start_SettingsActivity method");
        startActivity(new Intent(context, SailingRacePreferences.class));
    }
}
