package android.content;
public class Context {
 private final SharedPreferences prefs = new SharedPreferences();
 public SharedPreferences getSharedPreferences(String name, int mode) { return prefs; }
 public String getPackageName() { return "test"; }
 public android.content.pm.PackageManager getPackageManager() { return new android.content.pm.PackageManager(); }
}
