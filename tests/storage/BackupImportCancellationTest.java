package com.rareventure.gps2.gpx;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** An unresponsive cloud import must not trap Back/Cancel on the UI thread. */
public final class BackupImportCancellationTest {
    public static void main(String[] args) throws Exception {
        final File cache = Files.createTempDirectory("ttt-import-cancel").toFile();
        final BackupCancellation cancellation = new BackupCancellation();
        final CountDownLatch reading = new CountDownLatch(1), closing = new CountDownLatch(1);
        final CountDownLatch allowClose = new CountDownLatch(1), closed = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicReference<File> imported = new AtomicReference<>();
        final Thread caller = Thread.currentThread();
        InputStream provider = new InputStream() {
            public int read() { throw new AssertionError("Expected buffered reads"); }
            public int read(byte[] bytes, int start, int count) throws IOException {
                reading.countDown();
                BackupCancellationTest.await(closed);
                throw new IOException("provider closed");
            }
            public void close() throws IOException {
                check(Thread.currentThread() != caller, "Import cancellation closed the provider on the UI thread");
                closing.countDown();
                BackupCancellationTest.await(allowClose);
                closed.countDown();
            }
        };
        Thread worker = new Thread(() -> {
            try { imported.set(BackupDocumentStreams.stage(provider, cache, "cloud.zip", cancellation)); }
            catch (Throwable error) { failure.set(error); }
        });
        try {
            worker.start();
            check(reading.await(2, TimeUnit.SECONDS), "Provider did not enter its blocked read");
            long before = System.nanoTime();
            cancellation.cancel();
            check(System.nanoTime() - before < TimeUnit.MILLISECONDS.toNanos(500), "Import cancellation waited for blocked input");
            check(closing.await(2, TimeUnit.SECONDS), "Import cancellation did not close the blocked provider input");
            allowClose.countDown();
            worker.join(2000);
            check(!worker.isAlive(), "Import kept running after provider close");
            check(failure.get() instanceof IOException && imported.get() == null, "Cancelled import became a selected backup");
            check(cache.list().length == 0, "Cancelled import left its partial archive in cache");

            BackupCancellation early = new BackupCancellation(); early.cancel();
            try {
                BackupDocumentStreams.stage(new ByteArrayInputStream(new byte[]{1,2,3}), cache, "early.gpx", early);
                throw new AssertionError("Pre-cancelled import completed");
            } catch (IOException expected) { }
            check(cache.list().length == 0, "Pre-cancelled import left a temporary archive");
            cancelDuringSuccessfulClose(cache);
            System.out.println("BackupImportCancellationTest: blocked read/close, responsive cancellation and partial-file cleanup passed");
        } finally {
            allowClose.countDown();
            worker.join(4000);
            for (File file : cache.listFiles()) file.delete();
            cache.delete();
        }
    }
    static void cancelDuringSuccessfulClose(File cache) throws Exception {
        BackupCancellation token = new BackupCancellation();
        CountDownLatch closing = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicReference<File> imported = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InputStream provider = new ByteArrayInputStream(new byte[]{1,2,3}) {
            public void close() throws IOException {
                closing.countDown();
                BackupCancellationTest.await(release);
            }
        };
        Thread worker = new Thread(() -> {
            try { imported.set(BackupDocumentStreams.stage(provider, cache, "close.gpx", token)); }
            catch (Throwable error) { failure.set(error); }
        });
        worker.start();
        try {
            check(closing.await(2, TimeUnit.SECONDS), "Import never reached provider close");
            token.cancel();
        } finally { release.countDown(); }
        worker.join(2000);
        check(!worker.isAlive(), "Cancelled close did not finish");
        check(imported.get() == null && failure.get() instanceof IOException,
                "Cancellation during a successful provider close selected a backup");
        check(cache.list().length == 0, "Cancellation during provider close retained a staged archive");
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
