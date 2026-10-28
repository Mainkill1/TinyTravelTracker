package com.rareventure.android;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Exercises production callbacks, buffering and processing with Android boundary doubles. */
public class GpsReaderRegressionTest {
    static final long FIX_TIME = 1700000000123L;
    static class Fixture {
        final LocationManager manager = new LocationManager();
        final Context context = new Context(manager);
        final GpsReader reader;
        final ProcessThread processor;
        Fixture(GpsReader.GpsProcessor sink) {
            reader = new GpsReader(null, context, sink, "test", new Looper());
            processor = new ProcessThread(context, "test", reader);
            processor.setDaemon(true);
            reader.turnOn();
        }
        void emit(int id) {
            manager.listener.onLocationChanged(new Location(id, -id, id, FIX_TIME + id * 1000));
        }
        void drain() {
            int count = 0;
            while (reader.canProcess()) {
                check(++count <= 16, "queue must drain completely");
                reader.process();
            }
        }
    }
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static void timestamps() {
        List<Long> times = new ArrayList<>();
        Fixture f = new Fixture((lon, lat, alt, time) -> times.add(time));
        f.emit(0); f.emit(1); f.drain();
        check(GpsReader.getDiagnosticStatus().contains("last GPS fix"),
                "diagnostics distinguish a received fix from a running listener");
        check(times.size() == 2 && times.get(0) == FIX_TIME && times.get(1) == FIX_TIME + 1000,
                "delayed callbacks must preserve distinct 64-bit fix timestamps");
        f.reader.notifyShutdown();
    }
    static void overflow() {
        List<Integer> ids = new ArrayList<>();
        Fixture f = new Fixture((lon, lat, alt, time) -> ids.add((int) lat));
        for (int i = 0; i < 16; i++) f.emit(i);
        check(f.reader.canProcess(), "a full queue must not appear empty");
        f.drain();
        check(ids.size() == 15, "a 16-slot ring must retain 15 pending fixes and reserve one slot");
        for (int i = 0; i < 15; i++) check(ids.get(i) == i, "overflow must preserve queued fixes in order");
        f.emit(20); f.drain();
        check(ids.size() == 16 && ids.get(15) == 20, "reading must recover after overflow drains");
        f.reader.notifyShutdown();
    }
    static void blockedProcessor() throws Exception {
        List<Integer> ids = new ArrayList<>();
        CountDownLatch processing = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        Fixture f = new Fixture((lon, lat, alt, time) -> {
            ids.add((int) lat);
            if (lat == 0) {
                processing.countDown();
                try { check(resume.await(2, TimeUnit.SECONDS), "processor must resume"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            }
        });
        f.emit(0);
        f.processor.start();
        check(processing.await(1, TimeUnit.SECONDS), "processor must receive the first fix");
        Thread producer = new Thread(() -> { for (int i = 1; i < 20; i++) f.emit(i); });
        producer.setDaemon(true);
        producer.start(); producer.join(1000);
        check(!producer.isAlive(), "slow processing must not block Android's callback thread");
        resume.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (f.reader.canProcess() && System.nanoTime() < deadline) Thread.sleep(1);
        f.processor.notifyShutdown(); f.processor.join(1000);
        check(!f.processor.isAlive(), "processor must stop");
        check(ids.size() == 16, "processing one fix must leave room for 15 queued fixes");
        for (int i = 0; i < 16; i++) check(ids.get(i) == i, "queued coordinates must remain intact during processing");
    }
    static void explicitGpsRegistration() {
        Fixture f = new Fixture((lon, lat, alt, time) -> {});
        check(LocationManager.GPS_PROVIDER.equals(f.manager.requestedProvider),
                "recording must register GPS even if criteria choose network/fused");
        check(GpsReader.isProviderRegistered(), "diagnostics report successful registration");
        f.reader.turnOff();
        check(!GpsReader.isProviderRegistered(), "paused GPS must not be reported as registered");
        f.manager.requestFailure = new SecurityException("permission revoked during request");
        try { f.reader.turnOn(); } catch (SecurityException expected) {}
        check(!f.reader.isGpsOn() && !GpsReader.isProviderRegistered(),
                "failed registration must not leave gpsOn or diagnostics stuck true");
        f.manager.requestFailure = null;
        f.reader.turnOn();
        check(f.reader.isGpsOn() && f.manager.listener != null,
                "registration must retry after transient failure");
        f.reader.turnOff();
    }
    public static void main(String[] args) throws Exception {
        int failures = 0;
        for (int test = 0; test < 4; test++) {
            try {
                if (test == 0) timestamps();
                if (test == 1) overflow();
                if (test == 2) blockedProcessor();
                if (test == 3) explicitGpsRegistration();
                System.out.println("PASS reader regression " + test);
            } catch (AssertionError error) {
                failures++;
                System.out.println("FAIL reader regression " + test + ": " + error.getMessage());
            }
        }
        if (failures > 0) throw new AssertionError(failures + " reader regressions failed");
    }
}
