package android.support.v4.content;

import android.content.*;

public class ContextCompat {
  public static int starts;

  public static void startForegroundService(Context c, Intent i) {
    if (i.target != com.rareventure.gps2.GpsTrailerService.class)
      throw new AssertionError("Wrong service");
    starts++;
  }
}
