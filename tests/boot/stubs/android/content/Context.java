package android.content;

public class Context {
  public static final String JOB_SCHEDULER_SERVICE = "jobscheduler";
  public boolean persistedCollectData;
  public final android.app.job.JobScheduler scheduler = new android.app.job.JobScheduler();
  public int stops;

  public Object getSystemService(String name) {
    if (!JOB_SCHEDULER_SERVICE.equals(name)) throw new AssertionError("Wrong service: " + name);
    return scheduler;
  }

  public boolean stopService(Intent intent) {
    if (intent.target != com.rareventure.gps2.GpsTrailerService.class)
      throw new AssertionError("Wrong collector target");
    stops++;
    return true;
  }
}
