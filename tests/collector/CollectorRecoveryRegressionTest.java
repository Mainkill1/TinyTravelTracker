import android.app.job.JobParameters;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.rareventure.gps2.CollectorServicePolicy;
import com.rareventure.gps2.GTG;
import com.rareventure.gps2.JobSchedulerRestarterService;
import com.rareventure.gps2.bootup.GpsTrailerReceiver;

public class CollectorRecoveryRegressionTest {
  static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    GTG.Requirement.PREFS_LOADED.fulfilled = false;
    GTG.prefs.isCollectData = true;
    JobSchedulerRestarterService job = new JobSchedulerRestarterService();
    job.persistedCollectData = false;
    seedJobs(job);
    check(!job.onStartJob(new JobParameters()), "Job has no asynchronous work");
    check(job.broadcasts == 0, "Disabled persisted collection must not broadcast recovery");
    assertCancelled(job, "Disabled job callback");

    Context context = state(false, true, false);
    seedJobs(context);
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
    assertCancelled(context, "Boot with a stale persisted job");
    check(ContextCompat.starts == 0, "Disabled boot must not start collection");

    context = state(true, false, true);
    seedJobs(context);
    CollectorServicePolicy.restartCollector(context);
    check(context.stops == 1, "Disabling stops the existing collector");
    check(ContextCompat.starts == 0, "Disabling must not relaunch the foreground service");
    assertCancelled(context, "Saved settings with a live disabled preference");

    context = state(false, true, false);
    seedJobs(context);
    check(!CollectorServicePolicy.isCollectionEnabled(context),
        "Disabled collector startup reads persisted preferences");
    assertCancelled(context, "Disabled collector startup");
    CollectorServicePolicy.scheduleRestartJob(context);
    assertCancelled(context, "Disabled startup must not register a new job");

    GTG.prefs.isCollectData = true;
    GTG.Requirement.PREFS_LOADED.fulfilled = true;
    com.rareventure.gps2.GpsTrailerService.running=true;
    CollectorServicePolicy.restartCollector(context);
    com.rareventure.gps2.GpsTrailerService.running=false;
    check(ContextCompat.starts == 1, "Re-enabling starts collection despite older saved false");
    CollectorServicePolicy.scheduleRestartJob(context);
    JobInfo recovery = context.scheduler.jobs.get(1);
    check(recovery != null, "Re-enabled collector startup registers recovery again");
    check(recovery.component.target == JobSchedulerRestarterService.class,
        "Recovery job targets the actual restart service");
    check(recovery.persisted && recovery.interval == 900000L,
        "Enabled recovery retains its persisted 15 minute schedule");
    check(CollectorServicePolicy.isCollectionEnabled(context)
        && context.scheduler.jobs.containsKey(1),
        "Enabled collection preserves recovery independently of temporary tracking pauses");

    GTG.Requirement.PREFS_LOADED.fulfilled = false;
    GTG.prefs.isCollectData = false;
    job = new JobSchedulerRestarterService();
    job.persistedCollectData = true;
    seedJobs(job);
    check(!job.onStartJob(new JobParameters()), "Enabled job completes synchronously");
    check(job.broadcasts == 1 && job.lastBroadcast.target == GpsTrailerReceiver.class,
        "Enabled job sends one explicit recovery broadcast");
    check(job.scheduler.jobs.containsKey(1), "Enabled job preserves periodic recovery");
    check(!job.onStopJob(new JobParameters()), "Interrupted job needs no immediate retry");

    GTG.Requirement.PREFS_LOADED.fulfilled = true;
    GTG.prefs.isCollectData = false;
    job.onStartJob(new JobParameters());
    check(job.broadcasts == 1, "Live disabled preference prevents additional recovery broadcasts");
    assertCancelled(job, "Job sees unsaved live disabled preference");
    android.os.Build.VERSION.SDK_INT = 36;
    context = state(true, true, true);
    new GpsTrailerReceiver().onReceive(context, new Intent(context, GpsTrailerReceiver.class));
    check(ContextCompat.starts == 0, "Inexact alarm cannot launch a new FGS on Android 12+");
    context.background = false;
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
    check(ContextCompat.starts == 0, "Boot needs background location permission on modern Android");
    context.background = true;
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
    check(ContextCompat.starts == 1, "Boot exemption with all-time precise grant may recover");
    context.fine = false;
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
    check(ContextCompat.starts == 1, "Missing precise grant must prevent service creation");
    context.fine = true;
    ContextCompat.rejectStart = true;
    new GpsTrailerReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
    ContextCompat.rejectStart = false;
    CollectorServicePolicy.scheduleRestartJob(context);
    check(!context.scheduler.jobs.containsKey(1), "Obsolete periodic FGS restart job is cancelled on Android 12+");
    System.out.println("PASS: collector settings, startup, boot and periodic job recovery policy");
  }

  static Context state(boolean saved, boolean live, boolean loaded) {
    Context context = new Context();
    context.persistedCollectData = saved;
    GTG.prefs.isCollectData = live;
    GTG.Requirement.PREFS_LOADED.fulfilled = loaded;
    ContextCompat.starts = 0;
    return context;
  }

  static void seedJobs(Context context) {
    context.scheduler.schedule(new JobInfo.Builder(1,
        new ComponentName(context, JobSchedulerRestarterService.class)).build());
    context.scheduler.schedule(new JobInfo.Builder(27,
        new ComponentName(context, JobSchedulerRestarterService.class)).build());
  }

  static void assertCancelled(Context context, String trigger) {
    check(!context.scheduler.jobs.containsKey(1), trigger + " must cancel restart job ID 1");
    check(context.scheduler.jobs.containsKey(27), trigger + " must retain unrelated jobs");
  }
}
