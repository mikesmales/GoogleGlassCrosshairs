package com.mikesmales.googleglasscrosshairs.helpers;

import android.os.SystemClock;
import android.view.MotionEvent;

public class TouchHelper {

    private static float halfScreenX = 640 / 2;
    private static float halfScreenY = 360 / 2;

    /**
     *
     *  See list of meta states found here:
     *  developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
     *
     */

    public static MotionEvent getTouchDown() {

        long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis() + 100;

		int metaState = 0;
		MotionEvent motionEvent = MotionEvent.obtain(
		    downTime, 
		    eventTime, 
		    MotionEvent.ACTION_DOWN,
            halfScreenX,
            halfScreenY,
		    metaState
		);
		
		return motionEvent;
	}
	
	public static MotionEvent getTouchUp() {

        long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis() + 100;

		int metaState = 0;
		MotionEvent motionEvent = MotionEvent.obtain(
				downTime, 
				eventTime, 
				MotionEvent.ACTION_UP,
                halfScreenX,
                halfScreenY,
				metaState
				);
		
		return motionEvent;
	}
}
