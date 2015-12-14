package com.example.volkerpetersen.sailingrace;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;

/**
 * Created by Volker Petersen - November 2015
 */
public class MainActivity extends Activity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public Context appContext;
    public GlobalParameters para;
    private AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Offline");
        appContext = getApplicationContext();

        // initialize our Global Parameter class by calling the
        // Application class (see application tag in AndroidManifest.xml)
        para = (GlobalParameters) getApplicationContext();
        para.setWindwardFlag(false);
        para.setWindwardRace(false);
        para.setWindwardLat(Double.NaN);
        para.setWindwardLon(Double.NaN);
        para.setLeewardFlag(false);
        para.setLeewardRace(false);
        para.setLeewardLat(Double.NaN);
        para.setLeewardLon(Double.NaN);
        //Log.d(LOG_TAG, "initial setting of 'WindwardLat': "+para.getWindwardLat());
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
        para.setWindwardFlag(false);
        para.setWindwardRace(false);
        para.setWindwardLat(Double.NaN);
        para.setWindwardLon(Double.NaN);
        para.setLeewardFlag(false);
        para.setLeewardRace(false);
        para.setLeewardLat(Double.NaN);
        para.setLeewardLon(Double.NaN);
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
                startActivity(new Intent(appContext, SailingRacePreferences.class));
                return true;
            case R.id.race_mode:
                startActivity(new Intent(appContext, start_raceActivity.class));
                return true;
            case R.id.timer_mode:
                startActivity(new Intent(appContext, start_timerActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // class to handle the Timer button to start the Timer activity
    public void start_timerActivity(View view) {
        startActivity(new Intent(appContext, start_timerActivity.class));
    }

    // class to handle the Race button and start the race activity
    public void start_raceActivity(View view) {
        startActivity(new Intent(appContext, start_raceActivity.class));
    }

    // class to handle the Settings button to allow user to customize the program parameters
    public void start_settingsActivity(View view) {
        //Log.e(LOG_TAG, "Reached the start_SettingsActivity method");
        startActivity(new Intent(appContext, SailingRacePreferences.class));
    }

    /**
     * confirmQuit Method handles the "Quit" confirmation so that the user doesn't quit the race
     * activity by accidentially hitting the back button
     */
    private void confirmQuit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.QuitApp);
        builder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog,int id){
                // Go ahead quit this App

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


}
