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
        Nammu.requested == null && ContextCompat.starts == 0,
        "Existing precise grant requires no request or repeated restart");
    System.out.println("PASS: real GTGActivityHelper permission decisions (8 scenarios)");
  }
}
