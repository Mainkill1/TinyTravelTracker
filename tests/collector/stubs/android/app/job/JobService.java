package android.app.job;

import android.content.Context;
import android.content.Intent;

public abstract class JobService extends Context {
  public int broadcasts;
  public Intent lastBroadcast;

  public void sendBroadcast(Intent intent) {
    broadcasts++;
    lastBroadcast = intent;
  }

  public abstract boolean onStartJob(JobParameters parameters);
  public abstract boolean onStopJob(JobParameters parameters);
}
