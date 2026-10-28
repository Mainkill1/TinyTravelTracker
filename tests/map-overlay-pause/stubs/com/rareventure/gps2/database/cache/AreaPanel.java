package com.rareventure.gps2.database.cache;
public class AreaPanel {
    public static final int MAX_AP_UNITS = 65536;
    public static final int[] DEPTH_TO_WIDTH = {1,4,16,64,256,1024,4096,16384,65536};
    public static final int NUM_SUB_PANELS = 4;
    public int id;
    public int recordedStart = 10, recordedEnd = 20;
    public int getMaxX() { return 100; }
    public int getMaxY() { return 100; }
    public AreaPanel getSubAreaPanel(int index) { return null; }
    public boolean containsPoint(int x, int y) { return true; }
    public boolean outsideOfXY(AreaPanelSpaceTimeBox box) { return false; }
    public int getTimeTreeFk() { return 0; }
    public int getDepth() { return 0; }
    public int getX() { return 10; }
    public int getY() { return 20; }
    public int getCenterX() { return 10; }
    public int getCenterY() { return 20; }
    public int getStartTimeSec() { return recordedStart; }
    public int getEndTimeSec() { return recordedEnd; }
    public TimeTree getTimeTree() { return new TimeTree(); }
    public AreaPanel getChildApAtDepthAndTime(int depth, int time) { return this; }
    public static double convertXToLon(int x) { return x; }
    public static double convertYToLat(int y) { return y; }
    public static int convertLonToX(double longitude) { return (int) longitude; }
    public static int convertLatToY(double latitude) { return (int) latitude; }
}
