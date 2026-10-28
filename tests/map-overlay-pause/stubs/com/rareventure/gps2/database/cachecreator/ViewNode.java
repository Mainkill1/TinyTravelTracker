package com.rareventure.gps2.database.cachecreator;
import java.util.ArrayList;
import java.util.Map;
import com.rareventure.gps2.database.cache.*;
import com.rareventure.gps2.reviewer.map.ViewLine;
public class ViewNode {
    public int[] overlappingRange = {10,20};
    public int largestTimeTreeLengthForUncreatedLines;
    public AreaPanel ap() { return new AreaPanel(); }
    public void calcLinesForStBox(ArrayList<TimeTree> path, AreaPanelSpaceTimeBox box,
            Map<Integer, ViewLine> starts, Map<Integer, ViewLine> ends, int count) {}
}
