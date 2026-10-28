package com.mapzen.tangram;
import android.content.Context; import android.util.AttributeSet; import com.mapzen.tangram.networking.HttpHandler; import com.mapzen.tangram.viewholder.GLViewHolderFactory;
public class MapView {
 protected MapController mapController; private final Context context;
 public interface MapReadyCallback { void onMapReady(MapController c); }
 public MapView(Context c,AttributeSet a) { context=c; }
 public Context getContext() { return context; } public void onCreate(android.os.Bundle b) {} public void getMapAsync(MapReadyCallback c) {}
 protected void onLayout(boolean c,int l,int t,int r,int b) {} protected void onAttachedToWindow() {}
 public android.os.Handler getHandler() { return null; } public void invalidate() {} public void onPause() {} public void onResume() {} public int getWidth() { return 0; } public int getHeight() { return 0; }
 protected void onMapInitOnUIThread(MapController c,HttpHandler h,GLViewHolderFactory f,MapReadyCallback b) {} protected MapController getMapInstance() { return new MapController(); }
}
