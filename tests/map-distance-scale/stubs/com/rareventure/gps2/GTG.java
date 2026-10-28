package com.rareventure.gps2;
public class GTG { public static final String TAG="test"; public static final Lock cacheCreatorLock=new Lock(); public static class Lock { public void registerReadingThread() {} public void unregisterReadingThread() {} } public static java.io.File getExternalStorageDirectory() { return new java.io.File("."); } }
