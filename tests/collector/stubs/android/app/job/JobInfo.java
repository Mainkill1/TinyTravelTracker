package android.app.job;

import android.content.ComponentName;

public class JobInfo {
  public int id;
  public ComponentName component;
  public long interval;
  public boolean persisted;

  public static class Builder {
    private final JobInfo job = new JobInfo();

    public Builder(int id, ComponentName component) {
      job.id = id;
      job.component = component;
    }

    public Builder setPeriodic(long interval) { job.interval = interval; return this; }
    public Builder setPersisted(boolean persisted) { job.persisted = persisted; return this; }
    public JobInfo build() { return job; }
  }
}
