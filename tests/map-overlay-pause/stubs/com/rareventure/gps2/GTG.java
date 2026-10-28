package com.rareventure.gps2;
import com.rareventure.util.ReadWriteThreadManager;
import com.rareventure.gps2.database.cache.AreaPanel;
import com.rareventure.gps2.database.cachecreator.GpsTrailerCacheCreator;
public class GTG {
    public static final String TAG = "test";
    public static final int GPS_TRAILER_OVERLAY_DRAWER_PRIORITY = 1;
    public static ReadWriteThreadManager cacheCreatorLock;
    public static GpsTrailerCacheCreator cacheCreator;
    public static ApCache apCache = new ApCache();
    public static class ApCache {
        public static final int TOP_ROW_ID = 0;
        public final AreaPanel top = new AreaPanel();
        public boolean isDbFilled() { return true; }
        public AreaPanel getTopRow() { return top; }
        public AreaPanel getRow(int id) { return top; }
    }
}
