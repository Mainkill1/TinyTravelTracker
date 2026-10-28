package com.rareventure.gps2.reviewer.map;
public class OsmMapGpsTrailerReviewerMapActivity {
    public enum OngoingProcessEnum { DRAW_POINTS }
    public static class Preferences { public int[] colorRange = {0, 0xffffff}; }
    public static final Preferences prefs = new Preferences();
    public volatile int drawn, distanceUpdates;
    public Runnable afterDraw = () -> {};
    public final Runnable NOTIFY_HAS_DRAWN_RUNNABLE = () -> drawn++;
    public void notifyDoneProcessing(OngoingProcessEnum process) {}
    public void notifyProcessing(OngoingProcessEnum process) {}
    public void runOnUiThread(Runnable action) { action.run(); afterDraw.run(); }
    public void createNewUserLocation(int latitude, int longitude, float radius) {}
    public void notifyDistUpdated(double distance) { distanceUpdates++; }
    public void notifySelectedAreasChanged(boolean selected) {}
}
