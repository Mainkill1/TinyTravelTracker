package com.rareventure.gps2.database.cache;
public class TimeTree {
    public static int hackTimesLookingUpTimeTrees;
    public TimeTree getBottomLevelEncompassigTimeTree(int time) { return this; }
    public TimeTree getEncompassigTimeTreeOrMaxTimeTreeBeforeTime(int time, boolean flag) { return this; }
    public int calcTimeRangeCutEnd() { return 20; }
    public int calcTimeRangeCutStart() { return 10; }
    public int getMaxTimeSecs() { return 20; }
    public int getMinTimeSecs() { return 10; }
    public int getNextApId() { return 0; }
    public int getPrevApId() { return 0; }
}
