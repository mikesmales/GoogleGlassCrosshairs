package com.mikesmales.googleglasscrosshairs.helpers;

import android.os.Handler;
import android.os.Message;

public class TimerHandler extends Handler 
{  
	private Runnable runnable;
	
	public TimerHandler(Runnable aRunnable) {
		runnable = aRunnable;
	}
	
	@Override  
	public void handleMessage(Message msg) {
		runnable.run();
	}  

	public void sleep(long delayMillis) {
		this.removeMessages(0);  
		sendMessageDelayed(obtainMessage(0), delayMillis);  
	}  
};