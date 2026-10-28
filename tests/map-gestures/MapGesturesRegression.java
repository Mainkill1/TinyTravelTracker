package com.rareventure.gps2.reviewer.map;
import android.content.Context;
import android.view.MotionEvent;
import com.mapzen.tangram.*;

public class MapGesturesRegression {
 private static int failures;
 private static void check(boolean b,String message) { if(!b) { failures++; System.err.println("FAIL: "+message); } }
 private static MotionEvent event(int action) { return new MotionEvent(action,25,30); }
 private static class Responder implements MyTouchInput.LongPressResponder {
  final TouchInput sdk; int starts,ends,drags,cancels;
  Responder(TouchInput sdk) { this.sdk=sdk; }
  public void onLongPress(float x,float y) { starts++; check(sdk.cancelled,"SDK must be cancelled before custom ownership callback"); }
  public void onLongPressUp(float x,float y) { ends++; }
  public void onLongPressCancel() { cancels++; }
  public boolean onLongPressPan(float x,float y,float ex,float ey) { drags++; return true; }
 }
 public static void main(String[] args) throws Exception {
  MyMapController c=new MyMapController(new Context()); c.setupTouchListener();
  TouchInput.ScaleResponder scale=c.getTouchInput().scaleResponder;
  scale.onScaleBegin();
  // Tangram 0.10.1 quick-scale drift: factor .88 over 1ms gives -120/s.
  scale.onScale(25,30,.88f,-120f);
  check(c.scales==1 && c.scale==.88f && c.x==25 && c.y==30,"direct quick-scale must still reach renderer");
  check(c.velocity==0,"quick-scale drift must not launch zoom inertia");
  scale.onScale(25,30,1.2f,200f);
  check(c.scales==2 && c.scale==1.2f && c.velocity==0,"ordinary pinch must preserve direct scale without inertia");
  int before=c.scales;
  for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) scale.onScale(25,30,invalid,0);
  check(c.scales==before,"invalid scale factors must not reach renderer");
  scale.onScaleEnd(); check(c.begins==1 && c.ends==1,"scale lifecycle must reach renderer");

  TouchInput sdk=new TouchInput(); MyTouchInput input=new MyTouchInput(new Context(),sdk); Responder r=new Responder(sdk); input.setLongPressResponder(r);
  input.onTouch(null,event(MotionEvent.ACTION_DOWN)); input.onLongPress(event(MotionEvent.ACTION_DOWN));
  input.onTouch(null,event(MotionEvent.ACTION_MOVE)); input.onScroll(event(0),event(2),1,2);
  check(r.drags==1,"custom long-press drag must remain active");
  input.onTouch(null,event(MotionEvent.ACTION_CANCEL));
  input.onTouch(null,event(MotionEvent.ACTION_DOWN)); input.onTouch(null,event(MotionEvent.ACTION_MOVE)); input.onTouch(null,event(MotionEvent.ACTION_UP));
  check(sdk.events.toString().equals("[0, 3, 0, 2, 1]"),"CANCEL must release custom ownership and deliver next full stream: "+sdk.events);
  check(r.ends==0,"cancelled selection must not commit onLongPressUp");
  check(r.cancels==1,"CANCEL must discard selection preview exactly once");

  sdk=new TouchInput(); input=new MyTouchInput(new Context(),sdk); r=new Responder(sdk); input.setLongPressResponder(r);
  input.onTouch(null,event(0)); input.onLongPress(event(0)); input.onTouch(null,event(1));
  check(r.ends==1,"completed long press must end once");
  check(sdk.events.toString().equals("[0, 3]"),"custom UP must not leak into cancelled SDK stream: "+sdk.events);

  sdk=new TouchInput(); input=new MyTouchInput(new Context(),sdk); r=new Responder(sdk); input.setLongPressResponder(r);
  input.onTouch(null,event(0)); input.onLongPress(event(0)); input.onTouch(null,event(0)); input.onTouch(null,event(1));
  check(sdk.events.toString().equals("[0, 3, 0, 1]"),"fresh DOWN must recover even when terminal event was missing: "+sdk.events);
  check(r.ends==0,"new DOWN must discard stale custom selection");
  check(r.cancels==1,"fresh DOWN must discard stale selection preview exactly once");
  c=new MyMapController(new Context()); c.setupTouchListener(); r=new Responder(c.input);
  c.setLongPressResponderExt(r);
  c.handleGesture(new MapView(),event(0));
  java.lang.reflect.Field field=MyMapController.class.getDeclaredField("touchInput"); field.setAccessible(true);
  ((MyTouchInput)field.get(c)).onLongPress(event(0));
  c.handleGesture(new MapView(),event(3));
  check(r.cancels==1 && r.ends==0,"controller responder chain must forward cancellation without commit");
  if(failures>0) throw new AssertionError(failures+" gesture regression assertions failed");
  System.out.println("PASS: direct scales, invalid scales, ownership cancellation, drag, UP/CANCEL/new-DOWN recovery");
 }
}
