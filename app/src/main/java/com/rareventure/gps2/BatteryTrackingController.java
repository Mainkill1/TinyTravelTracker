package com.rareventure.gps2;

/** Coordinates GPS sessions across battery broadcasts without restarting the service. */
final class BatteryTrackingController {
    interface Listener {
        void startTracking();
        void stopTracking();
    }
    private final Listener listener;
    private boolean tracking;
    private boolean batteryLow;

    BatteryTrackingController(Listener listener) {
        this.listener = listener;
    }

    void update(int level, int scale, boolean charging, float cutoff) {
        // Missing extras and invalid scales must not spuriously resume GPS.
        if (level < 0 || scale <= 0 || level > scale) return;
        batteryLow = !charging && (float) level / scale < cutoff;
        boolean allowed = !batteryLow;
        if (allowed == tracking) return;
        if (allowed) listener.startTracking();
        else listener.stopTracking();
        tracking = allowed;
    }

    boolean isBatteryLow() {
        return batteryLow;
    }
}
