package com.rareventure.util;

import android.os.SystemClock;
import android.util.Log;

import com.rareventure.gps2.GTG;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/** Optional recording diagnostics, buffered to avoid flushing every message.
 * The next log call flushes after ten seconds or closes output when disabled.
 * A process exit can discard the buffered tail; this is diagnostic output only.
 */
public class DebugLogFile {
    private static final long FLUSH_INTERVAL_MS = 10_000;
    private static BufferedWriter debugOut;
    private static boolean triedOpeningAlready;
    private static long lastFlushMs;

    public static boolean isEnabled() {
        return GTG.prefs.writeFileLogDebug;
    }

    public static synchronized void log(String msg) {
        if (!isEnabled()) {
            closeOutput();
            return;
        }
        if (!openIfNeeded()) return;

        try {
            debugOut.write(new Date().toString());
            debugOut.write(": ");
            debugOut.write(msg);
            debugOut.newLine();
            long now = SystemClock.elapsedRealtime();
            if (now - lastFlushMs >= FLUSH_INTERVAL_MS) {
                debugOut.flush();
                lastFlushMs = now;
            }
        } catch (IOException e) {
            Log.e(GTG.TAG, "Couldn't write to debug log file", e);
        }
    }

    private static boolean openIfNeeded() {
        if (debugOut != null) return true;
        if (triedOpeningAlready) return false;
        triedOpeningAlready = true;
        try {
            File directory = GTG.getExternalStorageDirectory();
            if (directory == null) throw new IOException("App storage is unavailable");
            debugOut = new BufferedWriter(new FileWriter(new File(directory, "ttt_debug_log.txt"), true));
            lastFlushMs = SystemClock.elapsedRealtime();
            return true;
        } catch (IOException e) {
            Log.e(GTG.TAG, "Couldn't open app diagnostic log", e);
            return false;
        }
    }

    private static void closeOutput() {
        if (debugOut != null) {
            try {
                debugOut.close();
            } catch (IOException e) {
                Log.e(GTG.TAG, "Couldn't close debug log file", e);
            } finally {
                debugOut = null;
            }
        }
        triedOpeningAlready = false;
    }
}
