package com.rareventure.gps2;
public class GTG {
 public static final String TAG="test";
 public static final Preferences prefs=new Preferences();
 public static class Preferences { public boolean writeFileLogDebug; }
 public enum GTGEvent { ERROR_SERVICE_INTERNAL_ERROR }
 public static void alert(GTGEvent event) {}
}
