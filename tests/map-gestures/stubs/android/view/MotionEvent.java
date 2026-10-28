package android.view;
public class MotionEvent {
 public static final int ACTION_DOWN=0, ACTION_UP=1, ACTION_MOVE=2, ACTION_CANCEL=3;
 private int action; private final float x,y; public boolean recycled;
 public MotionEvent(int action,float x,float y) { this.action=action; this.x=x; this.y=y; }
 public static MotionEvent obtain(MotionEvent e) { return new MotionEvent(e.action,e.x,e.y); }
 public void setAction(int a) { action=a; } public int getActionMasked() { return action; }
 public float getX() { return x; } public float getY() { return y; }
 public float getX(int i) { return x; } public float getY(int i) { return y; }
 public int getPointerCount() { return 1; } public void recycle() { recycled=true; }
}
