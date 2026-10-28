package com.rareventure.gps2;
public class CollectorServicePolicy {
 public static boolean startFromActivity(android.content.Context c) {
  androidx.core.content.ContextCompat.startForegroundService(c, new android.content.Intent(c, GpsTrailerService.class));
  return true;
 }
}
