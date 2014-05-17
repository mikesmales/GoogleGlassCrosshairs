package com.mikesmales.googleglasscrosshairs.ui.fragments;


import com.google.android.glass.touchpad.Gesture;
import com.mikesmales.googleglasscrosshairs.R;
import com.mikesmales.googleglasscrosshairs.helpers.AudioNotification;
import com.mikesmales.googleglasscrosshairs.helpers.TouchHelper;
import com.mikesmales.googleglasscrosshairs.sensors.GlassOrientationObserver;
import com.mikesmales.googleglasscrosshairs.sensors.GlassOrientationTracker;
import com.mikesmales.googleglasscrosshairs.ui.components.ProgressBar;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class WebViewFragment extends Fragment {

    private GlassOrientationTracker orientationTracker;
    protected ProgressBar progressBar;
    protected WebView webView;
	private RelativeLayout crosshairView;

	private boolean inForeground;
	private String failingUrl;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return (RelativeLayout)inflater.inflate(R.layout.layout_webview, container, false);
	}
	
    @Override
    public void onResume() {
        super.onResume();
        inForeground = true;
        
        getActivity().runOnUiThread(new SetupWebViewUI());
    }
    
    @Override
    public void onPause() {
        super.onPause();
        inForeground = false;
    }
	
	private class SetupWebViewUI implements Runnable {

        public void run() {

			setupProgressBar();
	        setupWebview();
	        setupCrosshairs();
	        launchSite();
		}
	}

	private void setupProgressBar() {
        progressBar = (ProgressBar) getView().findViewById(R.id.view_progress_bar);
    }

    private void setupWebview() {

        webView = (WebView) getView().findViewById(R.id.webview);

        webView.setWebChromeClient(mWebChromeClient);
        webView.setWebViewClient(mWebViewClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setSaveFormData(false);

        webView.getSettings().setBuiltInZoomControls(true);
    }

    private void setupCrosshairs() {

        crosshairView = (RelativeLayout) getView().findViewById(R.id.include_crosshair);
        orientationTracker = new GlassOrientationTracker(getActivity(), orientationListener);
    }

    private void launchSite() {

        String url = "http://en.wikipedia.org/wiki/Google_Glass";
        webView.loadUrl(url);
    }
    
    private WebChromeClient mWebChromeClient = new WebChromeClient() {

        public void onProgressChanged(WebView view, int progress) {

            if (!inForeground)
                return;

            if (progress == 100) {
                progressBar.setVisibility(View.GONE);
            }
            else {
                showProgressBarIfHidden();

                progressBar.setPercentage(progress);
                progressBar.invalidate();
            }
        }

        private void showProgressBarIfHidden() {
            if ( progressBar.getVisibility() == View.GONE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    };

    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            WebViewFragment.this.failingUrl = failingUrl;
            Toast.makeText(getActivity(), "Failed to load " + failingUrl, Toast.LENGTH_LONG).show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
        	super.onPageFinished(view, url);
        }
    };




	
	private GlassOrientationObserver orientationListener = new GlassOrientationObserver() {

        private final int ORIENTATION_MOVEMENT_MULTIPLIER = 30;
		
		@Override
		public void onUpdate(final float[] gyroValues, final float[] gyroSum) {

            final float xGyro = gyroValues[1];
			final float deltaX = ORIENTATION_MOVEMENT_MULTIPLIER * xGyro;
			
			final float yGyro = gyroValues[0];
			final float deltaY = ORIENTATION_MOVEMENT_MULTIPLIER * yGyro;

            //values need to be negated
            int deltaPitch = (int) -deltaX;
            int deltaYaw = (int) -deltaY;

			Activity currActivity = getActivity();
			currActivity.runOnUiThread(new MoveWebview(deltaPitch, deltaYaw));
		}
	};
	
	private class MoveWebview implements Runnable {

        int deltaPitch;
		int deltaYaw;
		
		public MoveWebview(int deltaPitch, int deltaYaw) {
			this.deltaPitch = deltaPitch;
			this.deltaYaw = deltaYaw;
		}
		
		public void run() {

			
			if (webView != null)
				webView.scrollBy(this.deltaPitch, this.deltaYaw);
		}
	}
	
    public void showCrosshairs(){

        if ( crosshairView.getVisibility() == View.GONE) {
    		crosshairView.setVisibility(View.VISIBLE);
        }
    	orientationTracker.onResume();
    }
    
    public void hideCrosshairs() {

        if ( crosshairView.getVisibility() == View.VISIBLE) {

            crosshairView.setVisibility(View.GONE);
        }
    	orientationTracker.onPause();
    }

    protected void doClick() {

        webView.dispatchTouchEvent(TouchHelper.getTouchDown());
		webView.dispatchTouchEvent(TouchHelper.getTouchUp());
		
		AudioNotification.playNotificationTap(getActivity());
    }
    
	public void retryLoadingSite() {
        webView.loadUrl(failingUrl);
    }

    public boolean onGesture(Gesture gesture) {

        if (gesture == Gesture.TAP) {
            doClick();
            return true;
        }

        return false;
    }

    public boolean onScroll(float displacement, float delta, float velocity) {
        webView.scrollBy(0, (int) delta);
        return false;
    }
}
