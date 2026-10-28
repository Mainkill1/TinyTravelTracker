package android.app;

import android.content.Context;

public class AlertDialog {
  public static int message;

  public static class Builder {
    public Builder(Context c) {}

    public Builder setMessage(int m) {
      message = m;
      return this;
    }

    public Builder setPositiveButton(int id, Object listener) {
      return this;
    }

    public AlertDialog show() {
      return new AlertDialog();
    }
  }
}
