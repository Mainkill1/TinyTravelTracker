package pl.tajchert.nammu;

import android.Manifest;
import android.app.Activity;

public class Nammu {
  public static boolean fine, coarse;
  public static String[] requested;
  public static PermissionCallback callback;

  public static boolean hasPermission(Activity a, String p) {
    return p.equals(Manifest.permission.ACCESS_FINE_LOCATION) ? fine : coarse;
  }

  public static void askForPermission(Activity a, String p, PermissionCallback c) {
    askForPermission(a, new String[] {p}, c);
  }

  public static void askForPermission(Activity a, String[] p, PermissionCallback c) {
    requested = p;
    callback = c;
  }
}
