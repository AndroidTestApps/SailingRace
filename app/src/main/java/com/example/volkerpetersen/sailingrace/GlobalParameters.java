package com.example.volkerpetersen.sailingrace;

import android.widget.TextView;

/**
 * Created by Volker Petersen November 2015.
 */
public class GlobalParameters {
    public static double declination;
    public static double longitude;
    public static double latitude;
    public static double SOG=0.0;
    public static double COG=0.0;
    public static double avgSOG=0.0;
    public static double avgCOG=0.0;
    public static boolean leewardSet=false;         // true if Leeward location has been set
    public static boolean windwardSet=false;        // true if Windward location has been set
    public static double windwardLAT = Double.NaN;  // Lat of race course Windward mark
    public static double windwardLON = Double.NaN;  // Lon of race course Windward mark
    public static double leewardLAT = Double.NaN;   // Lat of race course Leeward mark
    public static double leewardLON = Double.NaN;   // Lon of race course Leeward mark
    public static boolean leewardRace = false;      // true when we race toward the Leeward Mark
    public static boolean windwardRace = false;     // true when we race toward the Windward Mark
    public static String test = "init";
    public static String gpsStatus;

    public GlobalParameters() {
        // TODO any possible initializations go here
    }
}
