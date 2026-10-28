package android.location;
import android.os.Looper;
public class LocationManager {
 public static final String GPS_PROVIDER="gps";
 public LocationListener listener;
 public boolean isProviderEnabled(String provider) { return true; }
 public String getBestProvider(Criteria criteria, boolean enabled) { return GPS_PROVIDER; }
 public void requestLocationUpdates(String provider, long interval, float distance, LocationListener listener, Looper looper) { this.listener=listener; }
 public void removeUpdates(LocationListener listener) { this.listener=null; }
}
