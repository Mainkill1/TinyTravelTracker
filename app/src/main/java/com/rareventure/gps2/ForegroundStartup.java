package com.rareventure.gps2;

import android.content.pm.ServiceInfo;

/** Boundary for discharging a foreground start after a permission/preference race. */
final class ForegroundStartup {
 interface Actions {
  void promote(int foregroundType);
  void stop();
 }
 static void stopUnstartedService(int sdk, Actions actions) {
  // API34+ forbids TYPE_NONE for a modern target. shortService needs no location grant.
  // Only that type is passed: combining it with location would retain location prerequisites.
  actions.promote(sdk >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
      : ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
  actions.stop();
 }
}
