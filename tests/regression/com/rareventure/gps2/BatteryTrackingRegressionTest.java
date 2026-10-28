package com.rareventure.gps2;

public class BatteryTrackingRegressionTest {
    static class Sessions implements BatteryTrackingController.Listener {
        int starts, stops;
        public void startTracking() { starts++; }
        public void stopTracking() { stops++; }
    }
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static void update(BatteryTrackingController c, int level, int scale, boolean charging, float cutoff) {
        c.update(level, scale, charging, cutoff, true);
    }
    public static void main(String[] args) {
        Sessions initiallyDisabled = new Sessions();
        BatteryTrackingController disabled = new BatteryTrackingController(initiallyDisabled);
        disabled.update(50, 100, false, .35f, false);
        check(initiallyDisabled.starts == 0, "disabled GPS at startup must not start a session");
        disabled.update(50, 100, false, .35f, true);
        check(initiallyDisabled.starts == 1, "enabling GPS after startup must start a session");
        Sessions sessions = new Sessions();
        BatteryTrackingController c = new BatteryTrackingController(sessions);
        update(c, 20, 100, false, .35f);
        check(sessions.starts == 0, "low battery at startup must never start GPS");
        for (int i = 0; i < 100; i++) update(c, 20, 100, false, .35f);
        check(sessions.starts == 0 && sessions.stops == 0, "repeated low battery events must stay idle");
        update(c, 35, 100, false, .35f);
        check(sessions.starts == 1 && !c.isBatteryLow(), "exact cutoff must allow tracking");
        update(c, 34, 100, false, .35f);
        check(sessions.stops == 1 && c.isBatteryLow(), "crossing cutoff must stop GPS once");
        update(c, 34, 100, false, .35f);
        check(sessions.stops == 1, "duplicate broadcasts must not stop twice");
        update(c, 10, 100, true, .35f);
        check(sessions.starts == 2, "charging must resume tracking below cutoff");
        update(c, 10, 100, false, .35f);
        check(sessions.stops == 2, "unplugging below cutoff must pause again");
        update(c, -1, -1, false, .35f);
        update(c, 50, 0, false, .35f);
        update(c, 101, 100, false, .35f);
        check(sessions.starts == 2 && c.isBatteryLow(), "invalid samples must retain paused state");
        update(c, 400, 1000, false, .35f);
        check(sessions.starts == 3, "non-percent scales must be normalized");
        update(c, -1, 100, false, .35f);
        check(sessions.stops == 2, "invalid samples must retain running state");
        update(c, 0, 100, false, 0f);
        check(sessions.stops == 2, "zero cutoff disables battery pause");
        update(c, 30, 100, false, .35f);
        update(c, 30, 100, false, .20f);
        check(sessions.starts == 4 && sessions.stops == 3, "updated cutoff must resume tracking");
        c.update(50, 100, false, .35f, false);
        check(sessions.stops == 4, "disabling GPS must pause the active session");
        c.update(50, 100, false, .35f, true);
        check(sessions.starts == 5, "re-enabling GPS must resume immediately");
        c.update(10, 100, false, .35f, false);
        c.update(10, 100, false, .35f, true);
        check(sessions.starts == 5, "re-enabling GPS must still respect the battery cutoff");
        System.out.println("PASS battery startup, cutoff, duplicates, charging, unplugging, invalid samples, scales, settings, provider recovery");
    }
}
