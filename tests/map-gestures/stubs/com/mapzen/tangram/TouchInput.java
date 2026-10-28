package com.mapzen.tangram;
import java.util.*; import android.view.*;
public class TouchInput implements View.OnTouchListener {
 public interface ScaleResponder { boolean onScaleBegin(); boolean onScale(float x,float y,float scale,float velocity); boolean onScaleEnd(); }
 public ScaleResponder scaleResponder; public final List<Integer> events=new ArrayList<>(); public boolean cancelled;
 public boolean onTouch(View v,MotionEvent e) { events.add(e.getActionMasked()); cancelled=e.getActionMasked()==MotionEvent.ACTION_CANCEL; return true; }
 public void setScaleResponder(ScaleResponder r) { scaleResponder=r; }
}
