package com.mikesmales.googleglasscrosshairs.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.mikesmales.googleglasscrosshairs.R;
import com.mikesmales.googleglasscrosshairs.ui.fragments.WebViewFragment;

public class MainActivity extends Activity {

    private GestureDetector gestureDetector;
    private WebViewFragment webViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gestureDetector = createGestureDetector();
        webViewFragment = new WebViewFragment();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, webViewFragment)
                    .commit();
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        if (gestureDetector != null) {
            return gestureDetector.onMotionEvent(event);
        }
        return false;
    }

    private GestureDetector createGestureDetector() {

        GestureDetector gestureDetector = new GestureDetector(this);
        gestureDetector.setBaseListener(new GestureListener());
        gestureDetector.setScrollListener(new GestureScrollListener());
        gestureDetector.setFingerListener(new GestureFingerListener());
        return gestureDetector;
    }

    private class GestureScrollListener implements GestureDetector.ScrollListener {

        @Override
        public boolean onScroll(float displacement, float delta, float velocity) {

            if (webViewFragment != null)
                return webViewFragment.onScroll(displacement, delta, velocity);

            return false;
        }
    }

    private class GestureFingerListener implements GestureDetector.FingerListener {

        @Override
        public void onFingerCountChanged(int previousCount, int currentCount) {

            if (currentCount == 2) {
                webViewFragment.showCrosshairs();
            }
            else if (previousCount == 2) {
                webViewFragment.hideCrosshairs();
            }
        }
    }

    private class GestureListener implements GestureDetector.BaseListener {

        @Override
        public boolean onGesture(Gesture gesture) {

            if (webViewFragment != null) {
                return webViewFragment.onGesture(gesture);
            }

            return false;
        }
    }

    public void onDestroy() {

        super.onDestroy();
        gestureDetector = null;
    }
}