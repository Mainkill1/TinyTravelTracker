package com.rareventure.gps2.reviewer.map;
import com.rareventure.gps2.database.cache.AreaPanel;
public class ViewLine {
    public int startTimeSec, endTimeSec, startApId, endApId;
    public ViewLine(int start, int end) { startTimeSec = start; endTimeSec = end; }
    public AreaPanel getStartAp() { return new AreaPanel(); }
    public AreaPanel getEndAp() { return new AreaPanel(); }
}
