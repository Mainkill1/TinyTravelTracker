package com.rareventure.gps2.gpx;
public final class RestoreCollectorRestartTest {
 public static void main(String[] args) {
  RestoreCollectorRestart policy = new RestoreCollectorRestart();
  final int[] starts={0};
  RestoreCollectorRestart.Starter accepted=()->{starts[0]++;return true;};
  policy.completed();
  policy.startIfResumed(true,accepted);
  check(starts[0]==0,"Home/background restore completion attempted a location FGS start");
  policy.startIfResumed(false,accepted);
  check(starts[0]==1,"Returning to visible restore screen did not restart collector");
  policy.startIfResumed(false,accepted);
  check(starts[0]==1,"Completed restore repeatedly restarted collector on resume");
  policy.completed();
  policy.startIfResumed(false,()->false);
  policy.startIfResumed(false,accepted);
  check(starts[0]==2,"A temporarily ineligible collector could never restart on a later resume");
  System.out.println("RestoreCollectorRestartTest: background deferral, foreground restart and eligibility retry passed");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
