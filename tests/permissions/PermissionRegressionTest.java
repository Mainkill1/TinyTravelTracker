import android.Manifest;
import android.app.*;
import androidx.core.content.ContextCompat;
import com.rareventure.gps2.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import pl.tajchert.nammu.*;

public class PermissionRegressionTest {
  static class Screen extends Activity implements IGTGActivity {
    public void doOnCreate(android.os.Bundle b) {}

    public void doOnResume() {}

    public void doOnPause(boolean b) {}

    public void superFinish() {}

    public void onBackPressed() {}

    public void performCancel() {}

    public int getRequirements() {
      return 0;
    }

    public void startInternalActivity(android.content.Intent i) {}

    public void startInternalActivityForResult(android.content.Intent i, int n) {}

    public void exitFromApp() {}
  }

  static void check(boolean b, String message) {
    if (!b) throw new AssertionError(message);
  }

  static void reset() {
    android.os.Build.VERSION.SDK_INT=26;
    try {
      java.lang.reflect.Field f=GTGActivityHelper.class.getDeclaredField("permissionRequestInFlight");
      f.setAccessible(true); f.setBoolean(null, false);
    } catch (Exception e) { throw new AssertionError(e); }
    Nammu.notifications = false; Nammu.background=false;
    AlertDialog.explanation=null;
    Nammu.fine = false;
    Nammu.coarse = false;
    Nammu.requested = null;
    Nammu.callback = null;
    GTG.userDoesntWantUsToHaveGpsPerm = false;
    GTG.error = true;
    ContextCompat.starts = 0;
    AlertDialog.message = 0;
  }

  static void request() throws Exception {
    Method m = GTGActivityHelper.class.getDeclaredMethod("getGpsAllowOrDeny");
    m.setAccessible(true);
    m.invoke(new GTGActivityHelper(new Screen(), 0));
  }

  public static void main(String[] args) throws Exception {
    reset();
    request();
    check(
        Nammu.requested.length == 2
            && Arrays.asList(Nammu.requested).contains(Manifest.permission.ACCESS_FINE_LOCATION)
            && Arrays.asList(Nammu.requested).contains(Manifest.permission.ACCESS_COARSE_LOCATION),
        "Request coarse and fine together");
    reset();
    request();
    Nammu.coarse = true;
    Nammu.callback.permissionRefused();
    check(
        GTG.error && ContextCompat.starts == 0,
        "Approximate-only must not start precise recording");
    check(
        AlertDialog.message == R.string.location_permission_approximate,
        "Explain approximate-only precise requirement");
    check(GTG.userDoesntWantUsToHaveGpsPerm, "Do not repeatedly prompt after refusal");
    reset();
    request();
    Nammu.callback.permissionRefused();
    check(GTG.error && ContextCompat.starts == 0, "Denial must not start recording");
    check(
        AlertDialog.message == R.string.location_permission_denied,
        "Explain total denial separately");
    reset();
    request();
    Nammu.fine = true;
    Nammu.coarse = true;
    Nammu.callback.permissionGranted();
    check(
        !GTG.error && ContextCompat.starts == 1, "Precise grant clears error and starts recovery");
    reset();
    request();
    Nammu.coarse = true;
    Nammu.callback.permissionGranted();
    check(
        GTG.error && ContextCompat.starts == 0,
        "Do not trust callback without checking actual fine grant");
    reset();
    GTG.userDoesntWantUsToHaveGpsPerm = true;
    Nammu.fine = true;
    Nammu.coarse = true;
    request();
    check(
        !GTG.userDoesntWantUsToHaveGpsPerm && !GTG.error && ContextCompat.starts == 1,
        "Recover after precise permission enabled in Settings");
    reset();
    GTG.userDoesntWantUsToHaveGpsPerm = true;
    request();
    check(Nammu.requested == null, "Suppressed denial must not prompt again");
    reset();
    Nammu.fine = true;
    Nammu.coarse = true;
    request();
    check(
        Nammu.requested == null && ContextCompat.starts == 1,
        "Existing precise grant resumes the collector from a visible activity");
    reset();
    android.os.Build.VERSION.SDK_INT=36;
    Nammu.fine=true; Nammu.coarse=true;
    Screen screen=new Screen();
    GTGActivityHelper helper=new GTGActivityHelper(screen, 0);
    Method permissionMethod=GTGActivityHelper.class.getDeclaredMethod("getGpsAllowOrDeny");
    permissionMethod.setAccessible(true);
    permissionMethod.invoke(helper);
    check(Nammu.requested.length == 1 && Nammu.requested[0].equals(Manifest.permission.POST_NOTIFICATIONS),
        "Notifications are requested separately after the precise grant");
    Nammu.callback.permissionRefused();
    check(AlertDialog.explanation != null && AlertDialog.explanation.contains("Allow all the time"),
        "Notification refusal still explains optional background restart permission");
    Nammu.requested=null; AlertDialog.explanation=null;
    permissionMethod.invoke(helper);
    check(Nammu.requested == null && AlertDialog.explanation == null,
        "Declining optional permissions must not prompt repeatedly");
    reset(); android.os.Build.VERSION.SDK_INT=29;
    Nammu.fine=true; Nammu.coarse=true;
    request();
    check(Nammu.requested.length == 1 && Nammu.requested[0].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        "Android 10 background permission is its own request after precise");
    Nammu.callback.permissionRefused();
    reset();
    new GTGActivityHelper(new Screen(), 0).onCreate(null);
    check(ContextCompat.starts == 0, "onCreate must not start a location FGS before precise permission");
    System.out.println("PASS: real GTGActivityHelper permission decisions (8 scenarios)");
  }
}
