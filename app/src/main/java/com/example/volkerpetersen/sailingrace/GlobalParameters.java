package com.example.volkerpetersen.sailingrace;

import android.app.Application;

/**
 * Created by Volker Petersen November 2015.
 *
 * Global parameters used by the various Activities to utilize common data values across the
 * Activities.
 *
 * see more here: http://androidexample.com/Global_Variable_Or_Application_Context_Variable_-_Android_Example/index.php?view=article_discription&aid=114&aaid=136
 */
public class GlobalParameters extends Application {
    private double declination;
    private double BoatLAT = Double.NaN;
    private double BoatLON = Double.NaN;
    private double WindwardLAT = Double.NaN;  // Lat of race course Windward mark
    private double WindwardLON = Double.NaN;  // Lon of race course Windward mark
    private double LeewardLAT = Double.NaN;   // Lat of race course Leeward mark
    private double LeewardLON = Double.NaN;   // Lon of race course Leeward mark
    private boolean leewardRace = false;      // true when we race toward the Leeward Mark
    private boolean windwardRace = false;     // true when we race toward the Windward Mark
    private boolean leewardSet = false;       // true if Leeward location has been set
    private boolean windwardSet = false;      // true if Windward location has been set
    private double TWD = 0.0d;
    private double SOG = 0.0d;
    private double COG = 0.0d;
    private double avgSOG = 0.0d;
    private double avgCOG = 0.0d;
    private String gpsStatus;
    private String BestTack;
    private double CourseOffset = 0.0d;        // 0.0 = Windward leg, 180.0 = Downwind leg
    private String tack = "stbd";             // Starboard or Port tack (stbd, port)
    private double TackGybe;                  // Tack or Gybe angle (negative for port tack, positive for stbd)

    public double getTWD() {
        return TWD;
    }

    public void setTWD(double x) {
        TWD = x;
    }

    public double getDeclination() {
        return declination;
    }

    public void setDeclination(double x) {
        declination = x;
    }

    public double getBoatLat() {
        return BoatLAT;
    }

    public void setBoatLat(double x) {
        BoatLAT = x;
    }

    public double getBoatLon() {
        return BoatLON;
    }

    public void setBoatLon(double x) {
        BoatLON = x;
    }

    public double getWindwardLat() {
        return WindwardLAT;
    }

    public void setWindwardLat(double x) {
        WindwardLAT = x;
    }

    public double getWindwardLon() {
        return WindwardLON;
    }

    public void setWindwardLon(double x) {
        WindwardLON = x;
    }

    public double getLeewardLat() {
        return LeewardLAT;
    }

    public void setLeewardLat(double x) {
        LeewardLAT = x;
    }

    public double getLeewardLon() {
        return LeewardLON;
    }

    public void setLeewardLon(double x) {
        LeewardLON = x;
    }

    public boolean getWindwardFlag() {
        return windwardSet;
    }

    public void setWindwardFlag(boolean x) {
        windwardSet = x;
    }

    public boolean getLeewardFlag() {
        return leewardSet;
    }

    public void setLeewardFlag(boolean x) {
        leewardSet = x;
    }

    public boolean getWindwardRace() {
        return windwardRace;
    }

    public void setWindwardRace(boolean x) {
        windwardRace = x;
    }

    public boolean getLeewardRace() {
        return leewardRace;
    }

    public void setLeewardRace(boolean x) {
        leewardRace = x;
    }

    public double getSOG() {
        return SOG;
    }

    public void setSOG(double x) {
        SOG = x;
    }

    public double getCOG() {
        return COG;
    }

    public void setCOG(double x) {
        COG = x;
    }

    public double getAvgSOG() {
        return avgSOG;
    }

    public void setAvgSOG(double x) {
        avgSOG = x;
    }

    public double getAvgCOG() {
        return avgCOG;
    }

    public void setAvgCOG(double x) {
        avgCOG = x;
    }

    public String getGpsStatus() {
        return gpsStatus;
    }

    public void setGpsStatus(String x) {
        gpsStatus = x;
    }

    public String getTack() {
        return tack;
    }

    public void setTack(String x) {
        tack = x;
    }

    public double getCourseOffset() {
        return CourseOffset;
    }

    public void setCourseOffset(double x) {
        CourseOffset = x;
    }

    public double getTackGybe() {
        return TackGybe;
    }

    public void setTackGybe(double x) {
        TackGybe = x;
    }

    public String getBestTack() {
        return BestTack;
    }

    public void setBestTack(String x) {
        BestTack = x;
    }
}