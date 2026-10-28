import android.content.*;
import android.support.v4.content.ContextCompat;
import com.rareventure.gps2.*;
import com.rareventure.gps2.bootup.GpsTrailerReceiver;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class BootRecoveryRegressionTest {
  static void check(boolean b, String message) {
    if (!b) throw new AssertionError(message);
  }

  static Context state(boolean saved, boolean stale) {
    Context c = new Context();
    c.persistedCollectData = saved;
    GTG.prefs.isCollectData = stale;
    GTG.Requirement.PREFS_LOADED.fulfilled = false;
    ContextCompat.starts = 0;
    return c;
  }

  static void bootThroughManifest(Context context) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    Element manifest =
        factory.newDocumentBuilder().parse("app/src/main/AndroidManifest.xml").getDocumentElement();
    String android = "http://schemas.android.com/apk/res/android";
    NodeList permissions = manifest.getElementsByTagName("uses-permission");
    boolean permitted = false;
    for (int i = 0; i < permissions.getLength(); i++)
      if (((Element) permissions.item(i))
          .getAttributeNS(android, "name")
          .equals("android.permission.RECEIVE_BOOT_COMPLETED")) permitted = true;
    check(permitted, "Boot receiver requires RECEIVE_BOOT_COMPLETED");
    NodeList receivers = manifest.getElementsByTagName("receiver");
    boolean registered = false;
    for (int i = 0; i < receivers.getLength(); i++) {
      Element receiver = (Element) receivers.item(i);
      if (!receiver.getAttributeNS(android, "name").equals(GpsTrailerReceiver.class.getName()))
        continue;
      check(
          !receiver.getAttributeNS(android, "enabled").equals("false"),
          "Boot receiver must be enabled");
      NodeList actions = receiver.getElementsByTagName("action");
      for (int j = 0; j < actions.getLength(); j++)
        if (((Element) actions.item(j))
            .getAttributeNS(android, "name")
            .equals(Intent.ACTION_BOOT_COMPLETED)) registered = true;
    }
    check(registered, "Register BOOT_COMPLETED for the recovery receiver");
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
  }

  public static void main(String[] args) throws Exception {
    Context context = state(false, true);
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
    check(
        ContextCompat.starts == 0,
        "Disabled collection must not start service after boot, even with stale enabled memory");
    context = state(true, false);
    bootThroughManifest(context);
    check(
        ContextCompat.starts == 1,
        "Enabled collection starts explicit foreground service after registered boot broadcast");
    context = state(false, true);
    bootThroughManifest(context);
    check(
        ContextCompat.starts == 0,
        "Registered boot broadcast respects persisted disabled collection");
    context = state(true, false);
    new GpsTrailerReceiver().onReceive(context, new Intent(context, GpsTrailerReceiver.class));
    check(
        ContextCompat.starts == 1,
        "Normal explicit alarm without action continues to restart enabled collection");
    context = state(false, true);
    new GpsTrailerReceiver().onReceive(context, new Intent(context, GpsTrailerReceiver.class));
    check(ContextCompat.starts == 0, "Explicit alarms cannot restart disabled collection");
    context = state(true, false);
    new GpsTrailerReceiver().onReceive(context, new Intent(GpsTrailerReceiver.class.getName()));
    check(ContextCompat.starts == 1, "Existing named recovery broadcast continues to work");
    context = state(true, false);
    GTG.Requirement.PREFS_LOADED.fulfilled = true;
    new GpsTrailerReceiver().onReceive(context, new Intent(context, GpsTrailerReceiver.class));
    check(
        ContextCompat.starts == 0 && !GTG.prefs.isCollectData,
        "Live disabled collection must win over an older saved enabled preference");
    context = state(false, true);
    GTG.Requirement.PREFS_LOADED.fulfilled = true;
    new GpsTrailerReceiver().onReceive(context, new Intent(context, GpsTrailerReceiver.class));
    check(
        ContextCompat.starts == 1 && GTG.prefs.isCollectData,
        "Live enabled collection must not be overwritten by an older saved disabled preference");
    System.out.println(
        "PASS: real GpsTrailerReceiver boot/alarm decisions (8 scenarios), manifest boot"
            + " registration");
  }
}
