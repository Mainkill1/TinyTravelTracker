package com.rareventure.gps2;

import android.app.Activity;
import android.content.Intent;

public class GTG {
  public static boolean userDoesntWantUsToHaveGpsPerm;
  public static int fulfilledRequirements;
  public static long lastGtgClosedMS;
  public static boolean error;

  public enum GTGEvent {
    ERROR_GPS_NO_PERMISSION
  }

  public static void alert(GTGEvent e, boolean b) {
    error = b;
  }

  public static final int REQUIRE_DB_READY_DB_DOESNT_EXIST = 1,
      REQUIRE_DECRYPT_OK = 0,
      REQUIRE_TIMMY_DB_IS_CORRUPT = 1,
      REQUIRE_TIMMY_DB_NEEDS_UPGRADING = 2,
      REQUIRE_TIMMY_DB_NEEDS_PROCESSING_TIME = 3;

  public static class Lock {
    public void registerWritingThread() {}

    public void unregisterWritingThread() {}
  }

  public static Lock initRwtm = new Lock();

  public enum Requirement {
    PREFS_LOADED,
    NOT_IN_RESTORE,
    NOT_TRIAL_WHEN_PREMIUM_IS_AVAILABLE,
    NOT_TRIAL_EXPIRED,
    SDCARD_PRESENT,
    SYSTEM_INSTALLED,
    DB_READY,
    DECRYPT,
    ENCRYPT,
    PASSWORD_ENTERED,
    TIMMY_DB_READY;

    public boolean isOn(int n) {
      return false;
    }

    public void reset() {}
  }

  public static void requireInitialSetup(Activity a, boolean b) {}

  public static void requirePrefsLoaded(Activity a) {}

  public static boolean requireNotInRestore() {
    return true;
  }

  public static Intent requireNotTrialWhenPremiumIsAvailable(Activity a) {
    return null;
  }

  public static boolean requireNotTrialExpired() {
    return true;
  }

  public static boolean requireSdcardPresent(Activity a) {
    return true;
  }

  public static boolean requireSystemInstalled(Activity a) {
    return true;
  }

  public static int requireDbReady() {
    return 0;
  }

  public static int requireEncryptAndDecrypt(String s) {
    return 0;
  }

  public static void requireEncrypt() {}

  public static boolean requirePasswordEntered(String s, long n) {
    return true;
  }

  public static int requireTimmyDbReady(boolean b) {
    return 0;
  }

  public static void setAppPasswordNotEntered() {}
}
