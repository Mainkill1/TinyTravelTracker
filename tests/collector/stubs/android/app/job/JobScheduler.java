package android.app.job;

import java.util.HashMap;
import java.util.Map;

public class JobScheduler {
  public final Map<Integer, JobInfo> jobs = new HashMap<>();

  public int schedule(JobInfo job) { jobs.put(job.id, job); return 1; }
  public void cancel(int id) { jobs.remove(id); }
}
