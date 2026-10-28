package android.app;

import android.content.Context;

public class AlertDialog {
  public static int message;
  public static String explanation;

  public static class Builder {
    public Builder(Context c) {}

    public Builder setMessage(int m) {
      message = m;
      return this;
    }

    public Builder setMessage(CharSequence m) { explanation=m.toString(); return this; }
    public Builder setPositiveButton(String id, android.content.DialogInterface.OnClickListener listener) { return this; }
    public Builder setNegativeButton(int id, Object listener) { return this; }
    public Builder setPositiveButton(int id, Object listener) {
      return this;
    }

    public AlertDialog show() {
      return new AlertDialog();
    }
  }
}
