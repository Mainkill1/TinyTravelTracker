package com.rareventure.android;
import android.os.SystemClock;
public class IntentTimer {
 public volatile boolean held, cancelled;
 public volatile int sleeps;
 public long overshoot = -1;
 public Runnable onSleep;
 public void acquireWakeLock() { held=true; }
 public void releaseWakeLock() { held=false; }
 public void cancel() { cancelled=true; }
 public void sleepUntil(long deadline) {
  sleeps++; held=false;
  if (overshoot >= 0) SystemClock.now=deadline+overshoot;
  if (onSleep != null) onSleep.run();
 }
}
