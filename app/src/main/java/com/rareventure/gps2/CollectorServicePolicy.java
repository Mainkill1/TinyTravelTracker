package com.rareventure.gps2;

import android.app.job.JobInfo;
import android.os.Build;
import android.util.Log;
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
        // An old service object can remain until onDestroy after stopService returns.
        if (isCollectionEnabled(context) && hasPreciseLocation(context)) start(context);
    }

    public static boolean hasPreciseLocation(Context context) {
        return ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION") == 0;
    }

    /** Call from a visible activity or an actual user action, after the precise grant. */
    public static boolean startFromActivity(Context context) {
        if (!isCollectionEnabled(context) || !hasPreciseLocation(context)) return false;
        return GpsTrailerService.notifyIfRunning() || start(context);
    }

    /** An inexact duty-cycle alarm may wake a live collector, not create a new FGS. */
    public static void recover(Context context, boolean bootCompleted) {
        if (!isCollectionEnabled(context) || !hasPreciseLocation(context)) return;
        if (GpsTrailerService.notifyIfRunning()) return;
        if (Build.VERSION.SDK_INT >= 31 && !bootCompleted) return;
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context,
                "android.permission.ACCESS_BACKGROUND_LOCATION") != 0) return;
        start(context);
    }

    private static boolean start(Context context) {
        try {
            ContextCompat.startForegroundService(context, new Intent(context, GpsTrailerService.class));
            return true;
        } catch (IllegalStateException | SecurityException e) {
            // Permission/state can change after the check. Opening the app can recover safely.
            Log.w("GpsTrailerService", "Collector start deferred; reopen app to resume recording", e);
            return false;
        }
    }

    public static void scheduleRestartJob(Context context) {
        if (!isCollectionEnabled(context)) return;
        if (Build.VERSION.SDK_INT >= 31) {
            // JobScheduler is not an exemption to modern foreground-service start restrictions.
            scheduler(context).cancel(RESTART_JOB_ID);
            return;
        }
        JobInfo job = new JobInfo.Builder(RESTART_JOB_ID,
                new ComponentName(context, JobSchedulerRestarterService.class))
                .setPeriodic(15 * 60 * 1000L).setPersisted(true).build();
        scheduler(context).schedule(job);
    }

    private static JobScheduler scheduler(Context context) {
        return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }
}
