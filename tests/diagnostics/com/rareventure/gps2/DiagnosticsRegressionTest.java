package com.rareventure.gps2;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import com.rareventure.android.GpsReader;
import com.rareventure.android.IntentTimer;
import com.rareventure.android.TestUtil;
import com.rareventure.android.WriteConstants;
import com.rareventure.util.DebugLogFile;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Uses production logging and scheduler; Android and file locations are boundaries. */
public class DiagnosticsRegressionTest {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static Object field(Object value, String name) throws Exception {
        Field f = value.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(value);
    }
    static void idle(Thread worker, GpsReader reader, int calls) throws Exception {
        long deadline = System.nanoTime() + 1000000000L;
        while ((reader.calls < calls || worker.getState() != Thread.State.TIMED_WAITING)
                && System.nanoTime() < deadline) Thread.sleep(1);
        check(reader.calls >= calls && worker.getState() == Thread.State.TIMED_WAITING,
                "worker must reach its next wait");
    }
    static void disabledWorker() throws Exception {
        GpsReader reader = new GpsReader();
        IntentTimer timer = new IntentTimer();
        GpsTrailerGpsStrategy strategy = new GpsTrailerGpsStrategy(null, reader, timer);
        Thread worker = (Thread) field(field(strategy, "gpsBatteryManager"), "strategyThread");
        worker.setDaemon(true);
        strategy.start();
        try {
            idle(worker, reader, 1);
            SystemClock.now += 2;
            strategy.gotReading();
            idle(worker, reader, 2);
            check(Log.errors == 0 && Log.warnings == 0,
                    "disabled diagnostics must emit no hot-path Logcat messages");
        } finally { strategy.shutdown(); }
    }
    static void enabledFile() throws Exception {
        GTG.prefs.writeFileLogDebug = true;
        Path log = GTG.getExternalStorageDirectory().toPath().resolve("ttt_debug_log.txt");
        DebugLogFile.log("first");
        DebugLogFile.log("second");
        check(Files.size(log) == 0, "debug file must buffer short adjacent messages");
        SystemClock.now += 11000;
        DebugLogFile.log("flush interval");
        String text = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        check(text.contains("first") && text.contains("second") && text.contains("flush interval"),
                "interval flush must retain every buffered line");
        DebugLogFile.log("pending before disable");
        GTG.prefs.writeFileLogDebug = false;
        DebugLogFile.log("disabled message");
        text = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        check(text.contains("pending before disable") && !text.contains("disabled message"),
                "disabling must close/flush buffered output without writing disabled messages");
        GTG.prefs.writeFileLogDebug = true;
        DebugLogFile.log("re-enabled");
        GTG.prefs.writeFileLogDebug = false;
        DebugLogFile.log("disabled again");
        text = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        check(text.contains("re-enabled"), "debug output must reopen after re-enabling");
    }
    static void explicitStream() throws Exception {
        int before = Log.warnings;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        TestUtil.writeMode(out, WriteConstants.MODE_WRITE_STRATEGY_STATUS);
        TestUtil.writeTime("fix", out, 1700000000123L);
        check(bytes.size() > 0, "explicit diagnostic output streams must remain supported");
        check(Log.warnings == before, "explicit streams must not also produce disabled Logcat output");
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("ttt-diagnostic-test");
        Environment.directory = directory.toFile();
        Path appFiles = Files.createDirectory(directory.resolve("app-files"));
        GTG.externalFilesDirectory = appFiles.toFile();
        int failures = 0;
        try {
            for (int test = 0; test < 3; test++) {
                try {
                    if (test == 0) disabledWorker();
                    if (test == 1) enabledFile();
                    if (test == 2) explicitStream();
                    System.out.println("PASS diagnostics regression " + test);
                } catch (AssertionError error) {
                    failures++;
                    System.out.println("FAIL diagnostics regression " + test + ": " + error.getMessage());
                    GTG.prefs.writeFileLogDebug = false;
                }
            }
        } finally {
            GTG.prefs.writeFileLogDebug = false;
            DebugLogFile.log("close");
            check(!Files.exists(directory.resolve("ttt_debug_log.txt")),
                    "logging must not write the restricted shared-storage root");
            Files.deleteIfExists(appFiles.resolve("ttt_debug_log.txt"));
            Files.deleteIfExists(appFiles);
            Files.deleteIfExists(directory);
        }
        if (failures != 0) throw new AssertionError(failures + " diagnostic regressions failed");
    }
}
