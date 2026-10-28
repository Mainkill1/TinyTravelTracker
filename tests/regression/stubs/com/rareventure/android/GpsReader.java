package com.rareventure.android;
public class GpsReader {
 public volatile int calls;
 public volatile boolean on;
 public void turn(boolean value) { on=value; calls++; }
 public void turnOff() { turn(false); }
}
