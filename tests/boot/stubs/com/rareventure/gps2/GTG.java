package com.rareventure.gps2;

import android.content.Context;

public class GTG {
  public static class Preferences {
    public boolean isCollectData;
  }

  public static Preferences prefs = new Preferences();

  public static class PreferenceSet {
    public void loadAndroidPreferencesFromSharedPrefs(Context context) {
      prefs.isCollectData = context.persistedCollectData;
    }
  }

  public static PreferenceSet prefSet = new PreferenceSet();
}
