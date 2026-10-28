package android.content;
public class Context {
 public static final String LOCATION_SERVICE="location";
 private final Object manager;
 public Context(Object manager) { this.manager=manager; }
 public Object getSystemService(String name) { return manager; }
}
