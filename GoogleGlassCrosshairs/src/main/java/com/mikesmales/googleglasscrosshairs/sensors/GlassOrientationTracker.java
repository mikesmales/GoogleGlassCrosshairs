package com.mikesmales.googleglasscrosshairs.sensors;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import static com.mikesmales.googleglasscrosshairs.sensors.MatrixHelper.checkForPositiveNegativeAngleMismatch;

/**
 *  Tracks head orientation using a sensor fusion approach.
 *
 *  Using just the accelerometer and magnetic compass together results in noise.
 *  Using solely the gyroscope results in drift.
 *
 *  Therefore, Orientation data from the accelerometer, magnetic compass, and gyro are combined
 *  in order to mitigate the effects of sensor error.
 */
public class GlassOrientationTracker {

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;

    final float ONE_MINUS_FILTER_COEFFICIENT = 1.0f - FILTER_COEFFICIENT;

    private SensorManager sensorManager;

	private float[] gyroAngularSpeeds = new float[3];
	private float[] gyroSum = new float[] { 0f, 0f, 0f };
	private float[] gyroRotationMatrix = new float[9];
	private float[] gyroOrientationAngles = new float[3];

	private float[] magnetometerArray = new float[3];
	private float[] accelerometerArray = new float[3];
	private float[] accMagOrientationAngles = new float[3];
	private float[] accMagRotationMatrix = new float[9];

	private float[] fusedOrientationAngles = new float[3];

    private Timer warmupTimer;
	private Handler callbackHandle;
	private GlassOrientationObserver observer;
	
	public GlassOrientationTracker(final Context aContext, GlassOrientationObserver observer) {

		this.observer = observer;

        initialiseGyroOrientationAngles();
        initialiseGyroRotationMatrixWithIdentityMatrix();

		sensorManager = (SensorManager) aContext.getSystemService(Activity.SENSOR_SERVICE);
		callbackHandle = new Handler();
	}

    private void initialiseGyroOrientationAngles() {

        gyroOrientationAngles[0] = 0.0f;
        gyroOrientationAngles[1] = 0.0f;
        gyroOrientationAngles[2] = 0.0f;
    }

    private void initialiseGyroRotationMatrixWithIdentityMatrix() {

        gyroRotationMatrix[0] = 1.0f;
        gyroRotationMatrix[1] = 0.0f;
        gyroRotationMatrix[2] = 0.0f;
        gyroRotationMatrix[3] = 0.0f;
        gyroRotationMatrix[4] = 1.0f;
        gyroRotationMatrix[5] = 0.0f;
        gyroRotationMatrix[6] = 0.0f;
        gyroRotationMatrix[7] = 0.0f;
        gyroRotationMatrix[8] = 1.0f;
    }


	public void onPause() {
        unregisterSensorsToSaveBatteryLife();
	}

    private void unregisterSensorsToSaveBatteryLife() {
        sensorManager.unregisterListener(sensorEventListener);
        warmupTimer.cancel();
    }

	public void onResume() {

		initialiseSensorListeners();
        waitForInitialSensorDataToBeCollected();
	}

    /**
     *   Wait for one second to initialise magnetometer/accelerometer
     *   Then schedule the sensor fusion task
     */
    private void waitForInitialSensorDataToBeCollected() {

        warmupTimer = new Timer();
        warmupTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 1000, TIME_CONSTANT);
    }


	public void initialiseSensorListeners() {

        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
	}


    private SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            switch (event.sensor.getType()) {

                case Sensor.TYPE_ACCELEROMETER:
                    processAccelerometerData(event);
                    calculateOrientationAnglesFromAccelrometerAndMagnetometerOutput();
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    processGyroscopeData(event);
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:
                    processMagometerData(event);
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    private void processAccelerometerData(SensorEvent event) {
        System.arraycopy(event.values, 0, accelerometerArray, 0, 3);

    }

    private void processMagometerData(SensorEvent event) {
        System.arraycopy(event.values, 0, magnetometerArray, 0, 3);
    }




    /**
     *  This Integrates the gyroscope data
     *  The gyroscope orientation data is written into the gyroOrientationAngles array
     */
	public void processGyroscopeData(SensorEvent event) {

		if (isAccelerometerMagnetometerOrientationDataEmpty())
			return;

		if (initState) {
            initGyroRotationMatrixUsingAccAndMagOrientationAngleData();
            initState = false;
		}

        populateGyroAngularSpeedsData(event);
	}

    private boolean isAccelerometerMagnetometerOrientationDataEmpty() {
        return (accMagOrientationAngles == null);
    }

    private void initGyroRotationMatrixUsingAccAndMagOrientationAngleData() {

        float[] initMatrix = new float[9];
        initMatrix = MatrixHelper.getRotationMatrixFromOrientation(accMagOrientationAngles);

        float[] temp = new float[3];
        SensorManager.getOrientation(initMatrix, temp);

        gyroRotationMatrix = MatrixHelper.matrixMultiplication(gyroRotationMatrix, initMatrix);
    }


    private void populateGyroAngularSpeedsData(SensorEvent event) {

        float[] deltaRotationVector = new float[4];

        if (timestamp != 0) {

            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyroAngularSpeeds, 0, 3);

            MatrixHelper.getRotationVectorFromGyro(gyroAngularSpeeds, deltaRotationVector, dT / 2.0f);

            gyroSum[0] += gyroAngularSpeeds[0];
            gyroSum[1] += gyroAngularSpeeds[1];
            gyroSum[2] += gyroAngularSpeeds[2];
        }

        timestamp = event.timestamp;

        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

        gyroRotationMatrix = MatrixHelper.matrixMultiplication(gyroRotationMatrix, deltaRotationMatrix);
        SensorManager.getOrientation(gyroRotationMatrix, gyroOrientationAngles);
    }


    private void calculateOrientationAnglesFromAccelrometerAndMagnetometerOutput() {

        if (SensorManager.getRotationMatrix(accMagRotationMatrix, null, accelerometerArray, magnetometerArray)) {
            SensorManager.getOrientation(accMagRotationMatrix, accMagOrientationAngles);
        }
    }



	private class calculateFusedOrientationTask extends TimerTask {

        public void run() {
            fuseSensorOrientationData();
		}
	}

    private void fuseSensorOrientationData() {

        fuseAzimuth();
        fusePitch();
        fuseRoll();

        gyroRotationMatrix = MatrixHelper.getRotationMatrixFromOrientation(fusedOrientationAngles);
        System.arraycopy(fusedOrientationAngles, 0, gyroOrientationAngles, 0, 3);

        notifyUI();
    }


    private void fuseAzimuth() {

        /**
         *  First check if one of the orientation angles is negative whilst the other is positive
         *  Compensate by adding (2 * math.PI) to the negative value, then perform the sensor fusion, and remove (2 * math.PI) from the result
         */

        if (checkForPositiveNegativeAngleMismatch(gyroOrientationAngles[0], accMagOrientationAngles[0])) {

            fusedOrientationAngles[0] = (float) (FILTER_COEFFICIENT * (gyroOrientationAngles[0] + 2.0 * Math.PI) + ONE_MINUS_FILTER_COEFFICIENT * accMagOrientationAngles[0]);
            fusedOrientationAngles[0] -= (fusedOrientationAngles[0] > Math.PI) ? 2.0 * Math.PI : 0;

        } else if (checkForPositiveNegativeAngleMismatch(accMagOrientationAngles[0], gyroOrientationAngles[0])) {

            fusedOrientationAngles[0] = (float) (FILTER_COEFFICIENT * gyroOrientationAngles[0] + ONE_MINUS_FILTER_COEFFICIENT * (accMagOrientationAngles[0] + 2.0 * Math.PI));
            fusedOrientationAngles[0] -= (fusedOrientationAngles[0] > Math.PI) ? 2.0 * Math.PI : 0;

        } else {
            fusedOrientationAngles[0] = FILTER_COEFFICIENT * gyroOrientationAngles[0] + ONE_MINUS_FILTER_COEFFICIENT * accMagOrientationAngles[0];
        }
    }

    private void fusePitch() {

        if (checkForPositiveNegativeAngleMismatch(gyroOrientationAngles[1], accMagOrientationAngles[1])) {
            fusedOrientationAngles[1] = (float) (FILTER_COEFFICIENT * (gyroOrientationAngles[1] + 2.0 * Math.PI) + ONE_MINUS_FILTER_COEFFICIENT * accMagOrientationAngles[1]);
            fusedOrientationAngles[1] -= (fusedOrientationAngles[1] > Math.PI) ? 2.0 * Math.PI : 0;

        } else if (checkForPositiveNegativeAngleMismatch(accMagOrientationAngles[1], gyroOrientationAngles[1])) {
            fusedOrientationAngles[1] = (float) (FILTER_COEFFICIENT * gyroOrientationAngles[1] + ONE_MINUS_FILTER_COEFFICIENT * (accMagOrientationAngles[1] + 2.0 * Math.PI));
            fusedOrientationAngles[1] -= (fusedOrientationAngles[1] > Math.PI) ? 2.0 * Math.PI : 0;

        } else {
            fusedOrientationAngles[1] = FILTER_COEFFICIENT * gyroOrientationAngles[1] + ONE_MINUS_FILTER_COEFFICIENT * accMagOrientationAngles[1];
        }
    }

    private void fuseRoll() {

        if (checkForPositiveNegativeAngleMismatch(gyroOrientationAngles[2], accMagOrientationAngles[2])) {
            fusedOrientationAngles[2] = (float) (FILTER_COEFFICIENT * (gyroOrientationAngles[2] + 2.0 * Math.PI) + ONE_MINUS_FILTER_COEFFICIENT * accMagOrientationAngles[2]);
            fusedOrientationAngles[2] -= (fusedOrientationAngles[2] > Math.PI) ? 2.0 * Math.PI : 0;

        } else if (checkForPositiveNegativeAngleMismatch(accMagOrientationAngles[2], gyroOrientationAngles[2])) {
            fusedOrientationAngles[2] = (float) (FILTER_COEFFICIENT * gyroOrientationAngles[2] + ONE_MINUS_FILTER_COEFFICIENT * (accMagOrientationAngles[2] + 2.0 * Math.PI));
            fusedOrientationAngles[2] -= (fusedOrientationAngles[2] > Math.PI) ? 2.0 * Math.PI : 0;

        } else {
            fusedOrientationAngles[2] = FILTER_COEFFICIENT * gyroOrientationAngles[2] + ONE_MINUS_FILTER_COEFFICIENT * accMagOrientationAngles[2];
        }
    }


    private void notifyUI() {
        callbackHandle.post(updateOrientationDisplayTask);
    }


	private Runnable updateOrientationDisplayTask = new Runnable() {
		public void run() {
			updateOrientationDisplay();
		}
	};

    public void updateOrientationDisplay() {
        observer.onUpdate(gyroAngularSpeeds, gyroSum);
    }
}