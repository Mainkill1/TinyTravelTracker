package com.rareventure.android;
public class GpsReader {
 public volatile int calls;
 public RuntimeException failure;
 public volatile boolean on;
 public void turn(boolean value) { if (value && failure != null) throw failure; on=value; calls++; }
 public void turnOff() { turn(false); }
}
