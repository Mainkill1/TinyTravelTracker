package com.rareventure.gps2.reviewer.map;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.mapzen.tangram.MapController;
import com.rareventure.android.SuperThread;
import com.rareventure.android.SuperThreadManager;
import com.rareventure.gps2.GTG;
import com.rareventure.gps2.database.cache.AreaPanelSpaceTimeBox;
import com.rareventure.gps2.database.cachecreator.GpsTrailerCacheCreator;
import com.rareventure.util.ReadWriteThreadManager;

/** Runs the production overlay, scheduler, and locks against host-only collaborators. */
public final class MapOverlayPauseRegression {
    public static void main(String[] args) throws Exception {
        pauseWithinRefinementBatch();
        pauseBetweenDrawingRounds();
        shutdownWhilePaused();
        System.out.println("Map overlay pause regressions passed (3 scenarios)");
    }

    private static void pauseWithinRefinementBatch() throws Exception {
        // A large redraw budget keeps repeated calculations inside the inner loop.
        try (Fixture fixture = new Fixture(60000, false)) {
            fixture.startAndAwaitPause();
            fixture.assertPausedWithoutLocks();
            fixture.manager.resumeAllSuperThreads();
            fixture.awaitFinishedDrawing();
            check(fixture.cache.calls == 20, "resume must finish the remaining refinements");
            check(fixture.controller.data.points == 3,
                    "resume must publish the location point and two existing corner markers");
            check(fixture.overlay.earliestOnScreenPointSec == 10
                    && fixture.overlay.latestOnScreenPointSec == 20,
                    "resume must preserve the visible time range");
            check(fixture.activity.distanceUpdates == 1,
                    "pause/resume must not repeat an up-to-date distance calculation");
        }
    }

    private static void pauseBetweenDrawingRounds() throws Exception {
        // A pass longer than the redraw budget forces the next outer round.
        try (Fixture fixture = new Fixture(50, true)) {
            fixture.startAndAwaitPause();
            fixture.assertPausedWithoutLocks();
            fixture.manager.resumeAllSuperThreads();
            fixture.awaitFinishedDrawing();
            check(fixture.cache.calls == 3, "outer rounds must continue after resume");
        }
    }

    private static void shutdownWhilePaused() throws Exception {
        try (Fixture fixture = new Fixture(60000, false)) {
            fixture.startAndAwaitPause();
            fixture.assertPausedWithoutLocks();
            fixture.shutdown();
            check(fixture.cache.calls == 1, "shutdown must not start more refinement passes");
            check(!GTG.cacheCreatorLock.isReadingThreadsActive(),
                    "shutdown must release the restored cache read registration");
        }
    }

    private static final class Fixture implements AutoCloseable {
        final SuperThreadManager manager = new SuperThreadManager();
        final SuperThread thread = new SuperThread(manager);
        final GpsTrailerCacheCreator cache = new GpsTrailerCacheCreator();
        final OsmMapGpsTrailerReviewerMapActivity activity = new OsmMapGpsTrailerReviewerMapActivity();
        final MapController controller = new MapController();
        final GpsTrailerOverlay overlay;
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        Fixture(long drawBudget, boolean delayPass) {
            GTG.cacheCreatorLock = new ReadWriteThreadManager();
            GTG.cacheCreator = cache;
            cache.manager = manager;
            cache.delayPass = delayPass;
            GpsTrailerOverlay.prefs.maxDrawCalcTime = drawBudget;
            overlay = new GpsTrailerOverlay(activity, thread, new OsmMapView());
            overlay.startTask(controller);
            overlay.notifyScreenChanged(new AreaPanelSpaceTimeBox());
            if (delayPass) {
                cache.pauseOnCall = 0;
                cache.totalCalls = 3;
                activity.afterDraw = () -> {
                    if (activity.drawn == 1) {
                        overlay.notifyViewNodesChanged();
                        manager.pauseAllSuperThreads();
                        cache.paused.countDown();
                    }
                };
            }
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((unused, throwable) -> failure.set(throwable));
        }

        void startAndAwaitPause() throws Exception {
            thread.start();
            check(cache.paused.await(2, TimeUnit.SECONDS), "first calculation or drawing round must request pause");
        }

        void assertPausedWithoutLocks() throws Exception {
            await(() -> thread.getState() == Thread.State.WAITING,
                    "overlay must reach its manager's pause wait");
            check(cache.calls == 1,
                    "paused overlay calculated " + cache.calls + " passes; expected 1");
            check(activity.distanceUpdates == 1,
                    "paused outer round must not begin another distance calculation");
            CountDownLatch wrote = new CountDownLatch(1);
            Thread writer = new Thread(() -> {
                GTG.cacheCreatorLock.registerWritingThread();
                try {
                    cache.viewNodeThreadManager.registerWritingThread();
                    try { wrote.countDown(); }
                    finally { cache.viewNodeThreadManager.unregisterWritingThread(); }
                } finally { GTG.cacheCreatorLock.unregisterWritingThread(); }
            }, "cache-writer-during-map-pause");
            writer.setDaemon(true);
            writer.start();
            check(wrote.await(2, TimeUnit.SECONDS),
                    "a paused overlay must not block either cache or view-node writers");
            writer.join(2000);
            check(!writer.isAlive(), "writer must release its locks");
            check(cache.calls == 1, "writer access must not resume the paused overlay");
        }

        void awaitFinishedDrawing() throws Exception {
            await(() -> controller.renders > 0 && activity.drawn > 0,
                    "resumed overlay must draw the completed view");
            await(() -> thread.getState() == Thread.State.TIMED_WAITING,
                    "completed overlay must sleep for a view change");
            check(failure.get() == null, "overlay thread failed: " + failure.get());
        }

        void shutdown() throws Exception {
            Thread shutdown = new Thread(manager::shutdownAllSuperThreads, "shutdown-map-worker");
            shutdown.setDaemon(true);
            shutdown.start();
            shutdown.join(2000);
            check(!shutdown.isAlive(), "shutdown must wake and stop a paused worker");
            thread.join(2000);
            check(!thread.isAlive(), "overlay worker must exit");
            check(failure.get() == null, "overlay thread failed: " + failure.get());
        }

        @Override public void close() throws Exception { shutdown(); }
    }

    private static void await(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(1);
        check(condition.getAsBoolean(), message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
