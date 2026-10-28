package com.rareventure.gps2.database.cache;
public class AreaPanel {
    public static final int MAX_AP_UNITS = 65536;
    public static final int[] DEPTH_TO_WIDTH = {1,4,16,64,256,1024,4096,16384,65536};
    public int id;
    public int getDepth() { return 0; }
    public int getX() { return 10; }
    public int getY() { return 20; }
    public int getCenterX() { return 10; }
    public int getCenterY() { return 20; }
    public int getStartTimeSec() { return 10; }
    public int getEndTimeSec() { return 20; }
    public TimeTree getTimeTree() { return new TimeTree(); }
    public AreaPanel getChildApAtDepthAndTime(int depth, int time) { return this; }
    public static double convertXToLon(int x) { return x; }
    public static double convertYToLat(int y) { return y; }
    public static int convertLonToX(double longitude) { return (int) longitude; }
    public static int convertLatToY(double latitude) { return (int) latitude; }
}
