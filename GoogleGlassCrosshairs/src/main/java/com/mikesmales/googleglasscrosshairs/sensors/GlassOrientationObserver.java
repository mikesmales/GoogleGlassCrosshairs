package com.mikesmales.googleglasscrosshairs.sensors;


public interface GlassOrientationObserver {

    void onUpdate(final float[] gyro, final float[] gyroSum);
}
