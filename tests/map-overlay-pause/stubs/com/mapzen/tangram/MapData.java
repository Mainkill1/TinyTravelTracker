package com.mapzen.tangram;
import java.util.Map;
public class MapData {
    public int points;
    public void clear() { points = 0; }
    public void addPoint(LngLat point, Map<String, String> properties) { points++; }
}
