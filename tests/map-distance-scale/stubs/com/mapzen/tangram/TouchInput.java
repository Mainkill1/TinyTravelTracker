package com.mapzen.tangram;
public class TouchInput {
 public interface ShoveResponder { boolean onShoveBegin(); boolean onShove(float d); boolean onShoveEnd(); }
 public interface RotateResponder { boolean onRotateBegin(); boolean onRotate(float x,float y,float r); boolean onRotateEnd(); }
 public interface TapResponder { boolean onSingleTapUp(float x,float y); boolean onSingleTapConfirmed(float x,float y); }
 public interface DoubleTapResponder { boolean onDoubleTap(float x,float y); }
 public void setShoveResponder(ShoveResponder r) {} public void setRotateResponder(RotateResponder r) {} public void setTapResponder(TapResponder r) {} public void setDoubleTapResponder(DoubleTapResponder r) {}
}
