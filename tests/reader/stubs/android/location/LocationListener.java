package android.location;
import android.os.Bundle;
public interface LocationListener {
 void onLocationChanged(Location location); void onProviderDisabled(String provider);
 void onProviderEnabled(String provider); void onStatusChanged(String provider, int status, Bundle extras);
}
