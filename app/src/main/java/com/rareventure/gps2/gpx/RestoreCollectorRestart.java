package com.rareventure.gps2.gpx;

/** Restoring may finish while the app is in the background, where location FGS starts are forbidden. */
final class RestoreCollectorRestart {
    interface Starter { boolean start(); }
    private boolean pending;

    void completed() { pending = true; }

    void startIfResumed(boolean paused, Starter starter) {
        if (!paused && pending && starter.start()) pending = false;
    }
}
