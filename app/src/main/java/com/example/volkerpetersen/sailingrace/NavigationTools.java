package com.example.volkerpetersen.sailingrace;

/**
 * Created by Volker Petersen November 2015.
 *
 * all angles are assumed to be True North
 */
import android.util.Log;

import java.text.DecimalFormat;

public class NavigationTools {
    private static final double km_nm = 0.539706;          /* km to nm conversion factor */
    private static final double Radius = 6371.0*km_nm;	    /* Earth Radius in nm */
    private static DecimalFormat df3 = new DecimalFormat("#0.000");
    static final String LOG_TAG = NavigationTools.class.getSimpleName();

    public static double fixAngle(double angle) {
        // computes an Angle between 0 and 359.99 degrees (full compass rose)
        return Mod(angle, 360.0);
    }

    public static Double HeadingDelta(double From, double To) {
        // difference between two angles going from angle "From" to angle "To"
        // Clockwise => Positive values 0-180,
        // Counter-Clockwise => Negative values -0 to -179.999999
        if (From > 360.0 || From < 0.0) {
            From = fixAngle(From);
        }
        if (To > 360.0 ||  To < 0.0) {
            To = fixAngle(To);
        }

        double diff = To - From;
        double absDiff = Math.abs(diff);

        if (absDiff <= 180) {
            if (absDiff == 180) {
                diff = absDiff;
            }
            return diff;
        } else if (To > From) {
            return absDiff - 360.0;
        }
        else {
            return 360.0 - absDiff;
        }
    }

    //-----------------------------------------------------------------------------------------
    // Method to compute the Velocity Made Good (VMG) toward the current active mark.
    // Inputs:
    //  (double) btm - Bearing-To-Mark
    //  (double) cog - Course-Over-Ground (both -btm & cog- assumed to be magnetic)
    //  (double) sog - Speed-Over-Ground
    // returns double vmg
    //-----------------------------------------------------------------------------------------
    public static double calcVMG(double btm, double cog, double sog) {
        double offcourse = HeadingDelta(btm, cog);
        double vmg = Math.cos(Math.toRadians(offcourse)) * sog;
        return vmg;
    }
    //-----------------------------------------------------------------------------------------
    // Method to compute the Velocity Made Good Upwind (VMGu) toward the current active mark.
    // This is the preferred VMG calc since it measures your upwind progress and avoids the
    // disadvantage of the conventional VMG that declines as you approach the mark layline.
    // (because the angle to the mark approaches 90 as you approach the layline, at which point vmg=0).
    // Inputs:
    //  (double) twa - True Wind Angle
    //  (double) CourseOffset - 0.0 for Upwind legs, 180.0 for Downwind legs
    //  (double) sog - Speed-Over-Ground
    // returns double vmgu as a percentage of the SOG
    //-----------------------------------------------------------------------------------------
    public static double calcVMGu(double twa, double CourseOffset, double sog) {
        double delta = HeadingDelta(CourseOffset, Math.abs(twa));
        double vmgu = Math.cos(Math.toRadians(delta));
        return vmgu;
    }

    //-----------------------------------------------------------------------------------------
    // Method to compute the optimum Laylines from current boat location to a mark
    // returns the waypoint at which to tack to sail the course Boat->Tack->Mark.
    // return array tack = [DTM, BTM, stbd (0) or port (1) initial tack]
    //
    // check drawing in Evernote "SailingRace App - Android"  - angle BTM renamed to Stbd
    //-----------------------------------------------------------------------------------------
    public static double[] optimumLaylines(double boatLat, double boatLon, double markerLat,
                                           double markerLon, double TWD, double TackAngle, String tack) {
        TackAngle = Math.abs(TackAngle);
        double[] results = new double[4];
        double[] DistanceBearing = MarkDistanceBearing(boatLat, boatLon, markerLat, markerLon);
        double DTM = DistanceBearing[0];
        double BTM = DistanceBearing[1];
        double delta = HeadingDelta(BTM, TWD);
        //TackAngle = Math.abs(TackAngle);
        double TlessD     = Math.toRadians(TackAngle - delta);  // equals theta_port
        double TplusD     = Math.toRadians(TackAngle + delta);  // equals theta_stbd
        double alpha_port  = Math.toRadians(90.0 - TackAngle - delta);
        double beta_port  = Math.toRadians(90.0 - TackAngle + delta);
        double theta_stbd = Math.toRadians(TackAngle + delta);  // equals TplusD
        double theta_port = Math.toRadians(TackAngle - delta);

        results[0] = Double.NaN;
        results[1] = Double.NaN;
        if (Math.abs(TackAngle) <= Math.abs(delta)) {
            // we can sail straight to the mark
            results[0] = markerLat;
            results[1] = markerLon;
            if (delta > 0.0) {
                // sail on starboard tack to mark
                results[2] = 0.0;
            } else {
                // sail on port tack to mark
                results[2] = 1.0;
            }
            results[3] = BTM;
        } else {
            // we have to put in a tack to reach the mark
            double d1_stbd = DTM * Math.tan(theta_stbd) / (Math.tan(TlessD)+Math.tan(theta_stbd));
            double d1_port = DTM * Math.tan(theta_port) / (Math.tan(TplusD)+Math.tan(theta_port));
            double d2_port = DTM - d1_port;
            double h1_port = d1_port/Math.cos(TplusD);
            double h2_port = d2_port/Math.sin(beta_port);
            double dist;
            double direction;
            String fav;
            if (h1_port > 1.5 * h2_port) {
                fav = "Port";
            } else if (h2_port > 1.5 * h1_port) {
                fav = "Starboard";
            } else {
                if (tack.equals("stbd")) {
                    fav = "Starboard_neutral";
                } else {
                    fav = "Port_neutral";
                }
            }
            if (fav.equals("Starboard") || fav.equals("Starboard_neutral")) {
                dist = d1_stbd/Math.cos(TlessD);
                direction = fixAngle(BTM - Math.toDegrees(TlessD));
                results[2] = 0.0d;
            } else {
                dist = d1_port/Math.cos(TplusD);
                direction = fixAngle(BTM + Math.toDegrees(TplusD));
                results[2] = 1.0d;
            }
            double [] latlon = withDistanceBearingToPosition(boatLat, boatLon, dist, direction);

            /* debug logs
            Log.d(LOG_TAG, "DTM       = "+df3.format(DTM));
            Log.d(LOG_TAG, "BTM       = "+df3.format(BTM));
            Log.d(LOG_TAG, "TWD       ="+df3.format(TWD));
            Log.d(LOG_TAG, "Delta     ="+df3.format(delta));
            Log.d(LOG_TAG, "TackAngle ="+df3.format(TackAngle));
            Log.d(LOG_TAG, "TlessD    ="+df3.format(Math.toDegrees(TlessD)));
            Log.d(LOG_TAG, "TplusD    ="+df3.format(Math.toDegrees(TplusD)));
            Log.d(LOG_TAG, "beta_port ="+df3.format(Math.toDegrees(beta_port)));
            Log.d(LOG_TAG, "theta_port="+df3.format(Math.toDegrees(theta_port)));
            Log.d(LOG_TAG, "Direction ="+df3.format((direction)));
            Log.d(LOG_TAG, "Distance  ="+df3.format(Math.toDegrees(dist)));
            Log.d(LOG_TAG, "d1_stbd   ="+df3.format(Math.toDegrees(d1_stbd)));
            Log.d(LOG_TAG, "d1_port   ="+df3.format(Math.toDegrees(d1_port)));
            Log.d(LOG_TAG, "h1_port   ="+df3.format(Math.toDegrees(h1_port)));
            Log.d(LOG_TAG, "h2_port   ="+df3.format(Math.toDegrees(h2_port)));
            double checksum = theta_port + theta_port + beta_port + (Math.toRadians(90.0)-theta_port);
            Log.d(LOG_TAG, "Checksum  ="+df3.format(Math.toDegrees(checksum)));
            */

            results[0] = latlon[0];
            results[1] = latlon[1];
            results[3] = direction;
        }
        return results;
    }

    //-----------------------------------------------------------------------------------------
    // Method to compute Distance (in nm) and Bearing (in degrees) to current active mark
    // using the Great Circle calculations.
    // returns array values = [Distance, Bearing]
    //-----------------------------------------------------------------------------------------
    public static double [] MarkDistanceBearing(double latFrom, double lonFrom, double latTo, double lonTo) {
        double[] values = new double[2];
        double dLat = Math.toRadians(latTo - latFrom);
        double dLon = Math.toRadians(lonTo-lonFrom);
        double lat1 = Math.toRadians(latFrom);
        double lat2 = Math.toRadians(latTo);

        double a = Math.sin(dLat / 2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        values[0] = Radius * c;          			// distance
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        values[1] = Math.toDegrees(Math.atan2(y, x));             // initial bearing
        values[1] = Mod(values[1], 360.0);      // final bearing adjusted to compass heading
        return values;
    }

    //-----------------------------------------------------------------------------------------
    // Method to compute the latitude and longitude when moving a set Distance (in nm) and
    // Bearing (in degrees) from a given location (latFrom, lonFrom)
    // returns array values = [Latitude, Longitude]
    //-----------------------------------------------------------------------------------------
    public static double [] withDistanceBearingToPosition(double latFrom, double lonFrom, double distance, double bearing) {
        double[] results = new double[2];
        double dist = distance / Radius;
        double brng = Math.toRadians(bearing);
        double lat1 = Math.toRadians(latFrom);
        double lon1 = Math.toRadians(lonFrom);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) + Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));
        double a = Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat1), Math.cos(dist) - Math.sin(lat1) * Math.sin(lat2));
        double lon2 = lon1 + a;
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;
        results[0] = Math.toDegrees(lat2);
        results[1] = Math.toDegrees(lon2);
        return results;
    }

    protected static double Mod(double a, double b) {
        /** Modulo function */
        while (a < 0) {
            a += b;
        }
        return a % b;
    }

    //-----------------------------------------------------------------------------------------
    // Method to convert a double position into a degree and decimal minutes string
    //-----------------------------------------------------------------------------------------
    public static String PositionDegreeToString(double wp, boolean flag) {
        DecimalFormat dfThree = new DecimalFormat("000");
        DecimalFormat dfTwo = new DecimalFormat("00");
        DecimalFormat df3 = new DecimalFormat("#0.000");
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
}
