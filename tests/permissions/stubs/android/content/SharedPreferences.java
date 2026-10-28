package android.content;
public class SharedPreferences {
 public final java.util.Map<String, Boolean> values = new java.util.HashMap<>();
 public boolean getBoolean(String key, boolean fallback) { return values.getOrDefault(key, fallback); }
 public Editor edit() { return new Editor(); }
 public class Editor {
  public Editor putBoolean(String key, boolean value) { values.put(key, value); return this; }
  public void apply() {}
 }
}
