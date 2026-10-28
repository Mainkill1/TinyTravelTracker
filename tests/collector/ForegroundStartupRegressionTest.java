package com.rareventure.gps2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.pm.ServiceInfo;

/** The Android boundary asserts promotion order, because stopSelf alone violates an accepted FGS start. */
public class ForegroundStartupRegressionTest {
 static void check(boolean b, String m) { if (!b) throw new AssertionError(m); }
 public static void main(String[] args) {
  for (int api : new int[]{21, 26, 29, 30, 33, 34, 36}) {
   final List<String> calls = new ArrayList<>();
   ForegroundStartup.stopUnstartedService(api, new ForegroundStartup.Actions() {
    public void promote(int type) { calls.add("promote:" + type); }
    public void stop() { calls.add("stop"); }
   });
   int cleanupType = api >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
       : ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE;
   check(calls.equals(Arrays.asList("promote:" + cleanupType, "stop")),
       "API " + api + " must discharge accepted foreground-start obligation before stopping: " + calls);
  }
  System.out.println("PASS foreground-start obligation cleanup at API21/26/29/30/33/34/36");
 }
}
