package com.rareventure.gps2.gpx;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class BackupCancellationTest {
    public static void main(String[] args) throws Exception {
        File archive = File.createTempFile("ttt-cancel", ".zip");
        Files.write(archive.toPath(), new byte[200000]);
        try {
            BackupCancellation early = new BackupCancellation();
            early.cancel();
            ByteArrayOutputStream untouched = new ByteArrayOutputStream();
            expectCanceled(archive, untouched, early);
            check(untouched.size() == 0, "Canceled export wrote into its provider destination");

            final BackupCancellation during = new BackupCancellation();
            final AtomicInteger copied = new AtomicInteger();
            OutputStream cancelAfterChunk = new OutputStream() {
                public void write(int value) { throw new AssertionError("Expected buffered writes"); }
                public void write(byte[] bytes, int start, int count) {
                    copied.addAndGet(count);
                    during.cancel();
                }
            };
            expectCanceled(archive, cancelAfterChunk, during);
            check(copied.get() <= 65536, "Cancel did not stop the next provider write");

            final BackupCancellation blocked = new BackupCancellation();
            final CountDownLatch writing = new CountDownLatch(1), closing = new CountDownLatch(1);
            final CountDownLatch allowClose = new CountDownLatch(1), closed = new CountDownLatch(1);
            final AtomicReference<Throwable> result = new AtomicReference<>();
            final Thread caller = Thread.currentThread();
            OutputStream provider = new OutputStream() {
                public void write(int value) { throw new AssertionError("Expected buffered writes"); }
                public void write(byte[] bytes, int start, int count) throws IOException {
                    writing.countDown();
                    await(closed);
                    throw new IOException("closed by cancellation");
                }
                public void close() throws IOException {
                    check(Thread.currentThread() != caller, "Provider close blocked the UI cancellation caller");
                    closing.countDown();
                    await(allowClose); // Even closing an unresponsive provider must happen off the UI thread.
                    closed.countDown();
                }
            };
            Thread worker = new Thread(() -> {
                try { BackupDocumentStreams.publish(archive, provider, blocked); }
                catch (Throwable failure) { result.set(failure); }
            });
            worker.start();
            check(writing.await(2, TimeUnit.SECONDS), "Worker did not enter provider write");
            long before = System.nanoTime();
            blocked.cancel();
            check(System.nanoTime() - before < TimeUnit.MILLISECONDS.toNanos(500), "Cancel waited for a blocked provider");
            check(closing.await(2, TimeUnit.SECONDS), "Cancel did not close the provider stream");
            allowClose.countDown();
            worker.join(2000);
            check(!worker.isAlive(), "Canceled provider transfer did not exit after stream close");
            check(result.get() instanceof IOException, "Canceled transfer incorrectly completed successfully");
            System.out.println("BackupCancellationTest: pre-cancel, between-chunk cancel and blocked-provider UI responsiveness passed");
        } finally { archive.delete(); }
    }
    static void expectCanceled(File archive, OutputStream destination, BackupCancellation token) throws Exception {
        try { BackupDocumentStreams.publish(archive, destination, token); throw new AssertionError("Canceled transfer succeeded"); }
        catch (IOException expected) { }
    }
    static void await(CountDownLatch signal) throws IOException {
        try { if (!signal.await(3, TimeUnit.SECONDS)) throw new IOException("test provider timeout"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
