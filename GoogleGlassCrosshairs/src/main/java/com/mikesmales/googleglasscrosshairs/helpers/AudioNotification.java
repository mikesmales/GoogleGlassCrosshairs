package com.mikesmales.googleglasscrosshairs.helpers;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.glass.media.Sounds;

public class AudioNotification 
{
	public static void playNotificationTap(Context context) {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
	}
	
	public static void playNotificationSuccess(Context context) {
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.SUCCESS);
	}
}