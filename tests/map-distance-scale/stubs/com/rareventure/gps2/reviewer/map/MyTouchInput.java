package com.rareventure.gps2.reviewer.map;
public class MyTouchInput { public interface LongPressResponder { void onLongPress(float x,float y); void onLongPressUp(float x,float y); default void onLongPressCancel() {} boolean onLongPressPan(float a,float b,float c,float d); } }
