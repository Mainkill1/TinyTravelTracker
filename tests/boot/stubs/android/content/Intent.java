package android.content;

public class Intent {
  public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
  public Class<?> target;
  private String action;

  public Intent() {}

  public Intent(String action) {
    this.action = action;
  }

  public Intent(Context c, Class<?> t) {
    target = t;
  }

  public String getAction() {
    return action;
  }
}
