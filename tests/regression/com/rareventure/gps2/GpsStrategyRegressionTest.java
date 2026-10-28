package com.rareventure.gps2;

import android.os.SystemClock;
import com.rareventure.android.GpsReader;
import com.rareventure.android.IntentTimer;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

/** Runs the production scheduling thread; only Android and I/O boundaries are doubles. */
public class GpsStrategyRegressionTest {
    static class Fixture {
        final GpsReader reader = new GpsReader();
        final IntentTimer timer = new IntentTimer();
        final GpsTrailerGpsStrategy strategy = new GpsTrailerGpsStrategy(null, reader, timer);
        final Thread thread;
        Fixture() throws Exception {
            SystemClock.now = 1000000;
            GpsTrailerGpsStrategy.prefs = new GpsTrailerGpsStrategy.Preferences();
            Object manager = field(strategy, "gpsBatteryManager");
            thread = (Thread) field(manager, "strategyThread");
            thread.setDaemon(true);
        }
        void start() throws Exception {
            strategy.start();
            await(() -> reader.calls > 0 && thread.getState() == Thread.State.TIMED_WAITING);
        }
        void stop() throws Exception {
            Thread stopper = new Thread(strategy::shutdown);
            stopper.setDaemon(true);
            stopper.start();
            stopper.join(1000);
            check(!stopper.isAlive(), "shutdown must complete even when requested during a wait");
            check(!timer.held, "shutdown must release wake lock");
            check(timer.cancelled, "shutdown must cancel the pending alarm");
            check(!reader.on, "shutdown must turn off GPS");
        }
    }
    static Object field(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 1000000000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(1);
        check(condition.getAsBoolean(), "timed out waiting for worker state");
    }
    static void overdueDeadline(long overshoot) throws Exception {
        Fixture f = new Fixture();
        f.timer.overshoot = overshoot;
        f.start();
        // A successful fix starts an off interval. Simulate alarm setup taking
        // exactly the remaining time, or longer, before Object.wait is reached.
        f.strategy.gotReading();
        await(() -> f.timer.sleeps > 0);
        Thread.sleep(30);
        check(f.thread.isAlive(), "overdue alarm must not kill GPS worker");
        check(f.reader.calls >= 3, "an expired deadline must run the next GPS cycle without waiting");
        f.stop();
    }
    static void earlyReadingBudget() throws Exception {
        Fixture f = new Fixture();
        f.start();
        SystemClock.now += 2;
        f.strategy.gotReading();
        await(() -> f.reader.calls >= 2);
        Object manager = field(f.strategy, "gpsBatteryManager");
        synchronized (f.strategy) {
            check((Long) field(manager, "totalTimeGpsRunningMs") == 2L,
                    "an early fix must charge two elapsed milliseconds, not the full GPS timeout");
        }
        f.stop();
    }
    static void shutdownDuringAlarmSetup() throws Exception {
        Fixture f = new Fixture();
        f.timer.onSleep = () -> {
            try {
                Field requested = f.strategy.getClass().getDeclaredField("isShutdownRequested");
                requested.setAccessible(true);
                requested.setBoolean(f.strategy, true);
            } catch (Exception e) { throw new AssertionError(e); }
        };
        f.start();
        f.strategy.gotReading();
        f.thread.join(1000);
        check(!f.thread.isAlive(), "shutdown during alarm setup must exit");
        f.stop();
    }
    static void continuousGps() throws Exception {
        Fixture f = new Fixture();
        GpsTrailerGpsStrategy.prefs.batteryGpsOnTimePercentage = 1f;
        f.start();
        SystemClock.now += 1000;
        f.strategy.gotReading();
        await(() -> f.reader.calls >= 2);
        check(f.reader.on, "100 percent GPS must stay enabled after a fix");
        f.stop();
    }
    public static void main(String[] args) throws Exception {
        int failures = 0;
        for (int test = 0; test < 6; test++) {
            try {
                if (test == 0) overdueDeadline(1);
                if (test == 1) overdueDeadline(0);
                if (test == 2) { Fixture f = new Fixture(); f.start(); f.stop(); }
                if (test == 3) earlyReadingBudget();
                if (test == 4) shutdownDuringAlarmSetup();
                if (test == 5) continuousGps();
                System.out.println("PASS GPS regression " + test);
            } catch (AssertionError e) {
                failures++;
                System.out.println("FAIL GPS regression " + test + ": " + e.getMessage());
            }
        }
        if (failures > 0) throw new AssertionError(failures + " GPS regressions failed");
    }
}
