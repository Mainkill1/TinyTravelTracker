package android.location;
import android.os.Looper;
public class LocationManager {
 public static final String GPS_PROVIDER="gps";
 public LocationListener listener;
 public String bestProvider="network", requestedProvider;
 public RuntimeException requestFailure;
 public int requests;
 public boolean isProviderEnabled(String provider) { return true; }
 public String getBestProvider(Criteria criteria, boolean enabled) { return bestProvider; }
 public void requestLocationUpdates(String provider, long interval, float distance, LocationListener listener, Looper looper) { requests++; if (requestFailure != null) throw requestFailure; this.requestedProvider=provider; this.listener=listener; }
 public void removeUpdates(LocationListener listener) { this.listener=null; }
}
