package android.app;

import android.content.*;

public class Activity extends Context {
  public Intent getIntent() {
    return new Intent();
  }

  public void finish() {}

  public boolean isTaskRoot() {
    return true;
  }

  public void startActivity(Intent i) {}

  public void startActivityForResult(Intent i, int r) {}
}
