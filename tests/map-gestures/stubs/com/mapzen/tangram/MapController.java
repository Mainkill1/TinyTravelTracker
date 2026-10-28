package com.mapzen.tangram;
import android.content.Context;
// Native renderer boundary. Capture arguments handed to Tangram's native pinch handler.
public class MapController {
 public final TouchInput input=new TouchInput(); public int begins,scales,ends; public float x,y,scale,velocity;
 public MapController(Context c) { input.setScaleResponder(getScaleResponder()); }
 public TouchInput getTouchInput() { return input; }
 public TouchInput.ScaleResponder getScaleResponder() { return new TouchInput.ScaleResponder() {
  public boolean onScaleBegin() { begins++; return true; }
  public boolean onScale(float px,float py,float s,float v) { scales++; x=px; y=py; scale=s; velocity=v; return true; }
  public boolean onScaleEnd() { ends++; return true; }
 }; }
}
