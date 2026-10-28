package android.location;
public class Location {
 private final double lat, lon, alt;
 private final long time;
 public Location(double lat, double lon, double alt, long time) { this.lat=lat; this.lon=lon; this.alt=alt; this.time=time; }
 public double getLatitude() { return lat; } public double getLongitude() { return lon; }
 public double getAltitude() { return alt; } public long getTime() { return time; }
}
