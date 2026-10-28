package com.rareventure.gps2.reviewer.map;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import com.mapzen.tangram.TouchInput;


/**
 * {@code TouchInput} collects touch data, applies gesture detectors, resolves simultaneous
 * detection, and calls the appropriate input responders.
 */
public class MyTouchInput implements OnTouchListener {

    private boolean longPressActive;
    private View touchView;

    /**
     * This is used to detect long presses. It is the same as panTapGestureDetector, but with
     * setIsLongpressEnabled() set to true. The problem is that after a longpress, GestureDetector
     * will mute onScroll messages until the long press is finished. This doesn't work well for
     * applications that use long press and onScroll events together (such as Tiny Travel Tracker).
     * <p>
     *     So by detecting long presses with this gesture detector, we are able to shutoff long
     *     press detection in the panTapGestureDetector, so it will still report onScroll messages
     *     even after a long press.
     * </p>
     */
    private final GestureDetector longPressGestureDetector;
    private final GestureDetector panGestureDetector;

    private TouchInput orig;

    public MyTouchInput(Context context, TouchInput onTouchListener) {
        this.orig = onTouchListener;

        this.longPressGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public void onLongPress(MotionEvent e) {
                MyTouchInput.this.onLongPress(e);
            }
        }
        );

        this.panGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return MyTouchInput.this.onScroll(e1, e2, distanceX, distanceY);
            }
        }
        );

        panGestureDetector.setIsLongpressEnabled(false);
    }

    public void onLongPress(MotionEvent e) {
        longPressActive = true;
        // End Tangram's stream as soon as custom selection takes ownership, so
        // its detectors cannot later interpret the release as a tap or fling.
        MotionEvent cancel = MotionEvent.obtain(e);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        try {
            orig.onTouch(touchView, cancel);
        } finally {
            cancel.recycle();
        }
        if (longPressResponder != null) {
            longPressResponder.onLongPress(e.getX(), e.getY());
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        touchView = view;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // Recover even if Android did not deliver the previous terminal event.
            cancelLongPress();
        }
        boolean customGesture = longPressActive;
        if (action == MotionEvent.ACTION_CANCEL) {
            // Cancellation discards selection; only a real UP commits it.
            cancelLongPress();
        } else if (action == MotionEvent.ACTION_UP && longPressActive) {
            longPressActive = false;
            if (longPressResponder != null) {
                longPressResponder.onLongPressUp(event.getX(), event.getY());
            }
        }

        longPressGestureDetector.onTouchEvent(event);
        panGestureDetector.onTouchEvent(event);

        if (!customGesture && !longPressActive) {
            orig.onTouch(view, event);
        }
        return true;
    }

    private void cancelLongPress() {
        if (longPressActive) {
            longPressActive = false;
            if (longPressResponder != null) {
                longPressResponder.onLongPressCancel();
            }
        }
    }

    /**
     * This is our long press responder. We add the ability to drag.
     */
    public interface LongPressResponder {
        /**
         * Called immediately after a long press is detected
         *
         * The duration threshold for a long press is determined by {@link ViewConfiguration}
         * @param x The x screen coordinate of the pressed point
         * @param y The y screen coordinate of the pressed point
         */
        void onLongPress(float x, float y);

        /**
         * Called when long press has stopped
         *
         * @param x The x screen coordinate of the pressed point
         * @param y The y screen coordinate of the pressed point
         */
        void onLongPressUp(float x, float y);

        /** Discard a cancelled long-press preview without committing selection. */
        void onLongPressCancel();

        /**
         * Called repeatedly while a touch point is dragged after a long press
         * @param startX The starting x screen coordinate for an interval of motion
         * @param startY The starting y screen coordinate for an interval of motion
         * @param endX The ending x screen coordinate for an interval of motion
         * @param endY The ending y screen coordinate for an interval of motion
         * @return True if the event is consumed, false if the event should continue to propagate
         */
        boolean onLongPressPan(float startX, float startY, float endX, float endY);
    }

    private LongPressResponder longPressResponder;

    /**
     * Set a {@link LongPressResponder}
     * @param responder The responder object, or null to leave these gesture events unchanged
     */
    public void setLongPressResponder(LongPressResponder responder) {
        this.longPressResponder = responder;
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if(longPressResponder == null || !longPressActive)
            return false;

        //copied from TouchInput
        float x = 0, y = 0;
        int n = e2.getPointerCount();
        for (int i = 0; i < n; i++) {
            x += e2.getX(i) / n;
            y += e2.getY(i) / n;
        }

        return longPressResponder.onLongPressPan(x + distanceX, y + distanceY, x, y);
    }

}
