package com.rareventure.gps2;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

/** Keeps collector recovery tied to the user's recording preference. */
public final class CollectorServicePolicy {
    private static final int RESTART_JOB_ID = 1;

    private CollectorServicePolicy() {}

    public static boolean isCollectionEnabled(Context context) {
        // Recovery can create a fresh process; preserve live settings once loaded.
        if (!GTG.Requirement.PREFS_LOADED.isFulfilled())
            GTG.prefSet.loadAndroidPreferencesFromSharedPrefs(context);
        if (GTG.prefs.isCollectData)
            return true;

        scheduler(context).cancel(RESTART_JOB_ID);
        return false;
    }

    public static void restartCollector(Context context) {
        context.stopService(new Intent(context, GpsTrailerService.class));
        if (isCollectionEnabled(context))
            ContextCompat.startForegroundService(context,
                    new Intent(context, GpsTrailerService.class));
    }

    public static void scheduleRestartJob(Context context) {
        if (!isCollectionEnabled(context))
            return;
        JobInfo job = new JobInfo.Builder(RESTART_JOB_ID,
                new ComponentName(context, JobSchedulerRestarterService.class))
                .setPeriodic(15 * 60 * 1000L).setPersisted(true).build();
        scheduler(context).schedule(job);
    }

    private static JobScheduler scheduler(Context context) {
        return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }
}
