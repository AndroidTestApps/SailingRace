package com.example.volkerpetersen.sailingrace;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Offline");
        //getActionBar().setIcon(R.mipmap.ic_launcher);
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
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, setting_Activity.class));
                return true;
            case R.id.race_mode:
                startActivity(new Intent(this, start_raceActivity.class));
                return true;
            case R.id.timer_mode:
                startActivity(new Intent(this, start_timerActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // class to handle the Timer button to start the Timer activity
    public void start_timerActivity(View view) {
        startActivity(new Intent(this, start_timerActivity.class));
    }

    // class to handle the Race button and start the race activity
    public void start_raceActivity(View view) {
        startActivity(new Intent(this, start_raceActivity.class));
    }

    // class to handle the Settings button to allow user to customize the program parameters
    public void start_settingsActivity(View view) {
        startActivity(new Intent(this, setting_Activity.class));
    }
}
