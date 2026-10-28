package android.content;

public class Intent {
  public Class<?> target;

  public Intent() {}

  public Intent(Context c, Class<?> t) {
    target = t;
  }

  public Intent putExtra(String k, int v) {
    return this;
  }

  public Intent putExtra(String k, boolean v) {
    return this;
  }

  public int getIntExtra(String k, int d) {
    return d;
  }
}
