package com.rareventure.gps2;
public class GTG {
 public enum GTGEvent { ERROR_SERVICE_INTERNAL_ERROR, ERROR_GPS_DISABLED, ERROR_GPS_NO_PERMISSION }
 public static volatile GTGEvent lastError;
 public static void alert(GTGEvent event) { lastError = event; }
 public static void alert(GTGEvent event, boolean on) { if (on) alert(event); }
}
