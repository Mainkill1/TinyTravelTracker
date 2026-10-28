package androidx.core.content;

import android.content.*;

public class ContextCompat {
  public static int starts;
  public static boolean rejectStart;
  public static int checkSelfPermission(Context c, String p) {
    return (p.equals("android.permission.ACCESS_FINE_LOCATION") ? c.fine : c.background) ? 0 : -1;
  }

  public static void startForegroundService(Context c, Intent i) {
    if (i.target != com.rareventure.gps2.GpsTrailerService.class)
      throw new AssertionError("Wrong service");
    if (rejectStart) throw new IllegalStateException("background start restricted");
    starts++;
  }
}
