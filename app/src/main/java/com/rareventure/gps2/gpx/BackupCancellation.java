package com.rareventure.gps2.gpx;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;

/** Cancellation never waits on a provider's write or close on the caller's thread. */
final class BackupCancellation {
    private volatile boolean cancelled;
    private Closeable active;

    boolean isCancelled() { return cancelled; }

    void check() throws InterruptedIOException {
        if (cancelled) throw new InterruptedIOException("Backup cancelled");
    }

    synchronized void register(Closeable stream) throws InterruptedIOException {
        check();
        active = stream;
    }

    synchronized void unregister(Closeable stream) {
        if (active == stream) active = null;
    }

    void cancel() {
        final Closeable stream;
        synchronized (this) {
            cancelled = true;
            stream = active;
            active = null;
        }
        if (stream != null) {
            Thread closer = new Thread(new Runnable() {
                @Override public void run() {
                    try { stream.close(); }
                    catch (IOException | RuntimeException ignored) { }
                }
            }, "Backup provider cancellation");
            closer.setDaemon(true);
            closer.start();
        }
    }
}
