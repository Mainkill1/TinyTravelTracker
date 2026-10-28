package com.rareventure.gps2.database.cache;
import java.util.List;
public class AreaPanelSpaceTimeBox {
    public int minZ = 10, maxZ = 20;
    public List<?> pathList;
    public AreaPanelSpaceTimeBox() {}
    public AreaPanelSpaceTimeBox(AreaPanelSpaceTimeBox other) {
        minZ = other.minZ; maxZ = other.maxZ; pathList = other.pathList;
    }
    public int getWidth() { return 4096; }
    public boolean contains(int x, int y) { return true; }
}
