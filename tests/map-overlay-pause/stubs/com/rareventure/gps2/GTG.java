package com.rareventure.gps2;
import com.rareventure.util.ReadWriteThreadManager;
import com.rareventure.gps2.database.cache.AreaPanel;
import com.rareventure.gps2.database.cachecreator.GpsTrailerCacheCreator;
public class GTG {
    public static final String TAG = "test";
    public static final int GPS_TRAILER_OVERLAY_DRAWER_PRIORITY = 1;
    public static ReadWriteThreadManager cacheCreatorLock;
    public static GpsTrailerCacheCreator cacheCreator;
    public static final ApCache apCache = new ApCache();
    public static class ApCache { public AreaPanel getTopRow() { return null; } }
}
