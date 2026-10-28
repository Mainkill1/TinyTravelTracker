package android.view;
import android.content.Context;
// Android detector boundary: tests deliver callbacks explicitly, with no timing simulation.
public class GestureDetector {
 public static class SimpleOnGestureListener { public void onLongPress(MotionEvent e) {} public boolean onScroll(MotionEvent a,MotionEvent b,float x,float y) { return false; } }
 public GestureDetector(Context c, SimpleOnGestureListener l) {} public void setIsLongpressEnabled(boolean b) {} public boolean onTouchEvent(MotionEvent e) { return false; }
}
