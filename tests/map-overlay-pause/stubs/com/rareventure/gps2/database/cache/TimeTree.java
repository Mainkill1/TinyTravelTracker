package com.rareventure.gps2.database.cache;
public class TimeTree {
    public static int hackTimesLookingUpTimeTrees;
    public static final int NUM_NODES = 4;
    public int getTimeSec() { return 10; }
    public boolean isBottomLevel() { return true; }
    public TimeTree getSubNode(int index) { return null; }
    public int getPrevApPrevTtEndTime() { return 10; }
    public int getNextApNextTtStartTime() { return 20; }
    public static void findOverlappingRange(int[] range, TimeTree tree, float fuzz) {}
    public TimeTree getBottomLevelEncompassigTimeTree(int time) { return this; }
    public TimeTree getEncompassigTimeTreeOrMaxTimeTreeBeforeTime(int time, boolean flag) { return this; }
    public int calcTimeRangeCutEnd() { return 20; }
    public int calcTimeRangeCutStart() { return 10; }
    public int getMaxTimeSecs() { return 20; }
    public int getMinTimeSecs() { return 10; }
    public int getNextApId() { return 0; }
    public int getPrevApId() { return 0; }
}
