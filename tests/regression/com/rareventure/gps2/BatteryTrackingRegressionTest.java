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
    public static void main(String[] args) {
        Sessions sessions = new Sessions();
        BatteryTrackingController c = new BatteryTrackingController(sessions);
        c.update(20, 100, false, .35f);
        check(sessions.starts == 0, "low battery at startup must never start GPS");
        for (int i = 0; i < 100; i++) c.update(20, 100, false, .35f);
        check(sessions.starts == 0 && sessions.stops == 0, "repeated low battery events must stay idle");
        c.update(35, 100, false, .35f);
        check(sessions.starts == 1 && !c.isBatteryLow(), "exact cutoff must allow tracking");
        c.update(34, 100, false, .35f);
        check(sessions.stops == 1 && c.isBatteryLow(), "crossing cutoff must stop GPS once");
        c.update(34, 100, false, .35f);
        check(sessions.stops == 1, "duplicate broadcasts must not stop twice");
        c.update(10, 100, true, .35f);
        check(sessions.starts == 2, "charging must resume tracking below cutoff");
        c.update(10, 100, false, .35f);
        check(sessions.stops == 2, "unplugging below cutoff must pause again");
        c.update(-1, -1, false, .35f);
        c.update(50, 0, false, .35f);
        c.update(101, 100, false, .35f);
        check(sessions.starts == 2 && c.isBatteryLow(), "invalid samples must retain paused state");
        c.update(400, 1000, false, .35f);
        check(sessions.starts == 3, "non-percent scales must be normalized");
        c.update(-1, 100, false, .35f);
        check(sessions.stops == 2, "invalid samples must retain running state");
        c.update(0, 100, false, 0f);
        check(sessions.stops == 2, "zero cutoff disables battery pause");
        c.update(30, 100, false, .35f);
        c.update(30, 100, false, .20f);
        check(sessions.starts == 4 && sessions.stops == 3, "updated cutoff must resume tracking");
        System.out.println("PASS battery startup, cutoff, duplicates, charging, unplugging, invalid samples, scales, settings");
    }
}
