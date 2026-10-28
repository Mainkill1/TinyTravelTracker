package com.mapzen.tangram;
import android.graphics.PointF;
public class MapController {
    public final MapData data = new MapData();
    public volatile int renders;
    public MapData addDataLayer(String name) { return data; }
    public LngLat screenPositionToLngLat(PointF point) { return new LngLat(); }
    public void requestRender() { renders++; }
}
