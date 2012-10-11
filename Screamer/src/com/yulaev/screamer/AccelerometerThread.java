/** This Thread deals with receiving updates from the system accelerometer, interprets and processes
 * the updates and send messages describing what is going on! It does some simple signal processing
 * on the accelerometer raw results, of course. 
 */

package com.yulaev.screamer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class AccelerometerThread extends Thread implements SensorEventListener{
	private boolean running;
	
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    final String activitynametag="AccelerometerThread";
    Handler handler;
	
	public AccelerometerThread (SensorManager sensorManager) {
		//mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensorManager = sensorManager;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        running = true;
        	
		last_samples_abs = new double[LAST_SAMPLE_DEPTH];
		last_samplecount = 0;
		
		falling_timeout = -1;		
        
        handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == MessageRouter.MESSAGE_TOGGLE_ACCELEROMETER_CALIBRATION) {					
					//Reset calibration
					last_samplecount = 0;
					Log.i(activitynametag, "Calibration reset.");					
				}
			}
		};
		
		MessageRouter.accelerometerThreadHandler = handler;
	}

	final int MESSAGE_DELAY = 100;
	
	/** Responsible for registering this Thread as an accelerometer sensor listener and sending
	 * periodic updates to the main Thread regarding whether we are in calibration mode or not.
	 */
	@Override
	public void run() {
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		while(running) {			
			if(last_samplecount >= LAST_SAMPLE_DEPTH) 
				MessageRouter.sendCalibratingMessage(false);
			else 
				MessageRouter.sendCalibratingMessage(true);
			
			try { Thread.sleep(MESSAGE_DELAY); }
			catch(InterruptedException e) {;}
		}
		
		mSensorManager.unregisterListener(this);

	}
	
	public void terminate() {
		running = false;
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// can be safely ignored for this demo
	}
	
	private final int LAST_SAMPLE_DEPTH = 8;
	private double [] last_samples_abs;
	private int last_samplecount;
		
	/** When we get a new set of samples from the accelerometer, we can use this to update
	 * our calibration.
	 * @param x
	 * @param y
	 * @param z
	 */
	private void updateLastSamples(float x, float y, float z) {
		if(last_samplecount < LAST_SAMPLE_DEPTH) {
			last_samples_abs[last_samplecount] = vectorLen(x,y,z);		
			last_samplecount++;
		}
		
		else {
			for(int i = 0; i < LAST_SAMPLE_DEPTH-1; i++) {
				last_samples_abs[i] = last_samples_abs[i+1];
			}
			
			last_samples_abs[LAST_SAMPLE_DEPTH-1] = vectorLen(x,y,z);
		}
	}
	
	/** Return the absolute vector length that we got when we calibrated the accelerometer. Used 
	 * to determine the relative offset of the current (absolute) device acceleration vs what we
	 * had measured in calibration.
	 * @return
	 */
	private double getOffsetAbs() {
		if(last_samplecount < LAST_SAMPLE_DEPTH) return(0.0d);
		else {
			double retval = 0.0f;
			for(int i = 0; i < LAST_SAMPLE_DEPTH-1; i++)
				retval += last_samples_abs[i];
			retval /= last_samplecount;
			return(retval);
		}
	}
	
	long falling_timeout;
	final long MIN_TIME_BETWEEN_FALLS_MS = 100;
	
	final int STATE_FALLING = 1;
	final int STATE_NOT_FALLING = 2;
	int state;
	
	/** Compute the lenght of a 3D vector */
	private float vectorLen(float x, float y, float z) {
		return ( (float) Math.sqrt(Math.pow(x,2.0) + Math.pow(y,2.0) + Math.pow(z,2.0)) );
	}
	
	/** Called when we get updated accelerometer sensor data. Process the result and either use it for calibration,
	 * or use it to determine wehter we are in normal earth gravity or greater, or if we are in freefall (relatively 
	 * low acceleration compared to during calibration).
	 */
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];	
		
		if(last_samplecount >= LAST_SAMPLE_DEPTH) {		
			if(state == STATE_NOT_FALLING && vectorLen(x,y,z) < getOffsetAbs()/2.0) {
				if(falling_timeout + MIN_TIME_BETWEEN_FALLS_MS < SystemClock.uptimeMillis()) {
					MessageRouter.sendFallingMessage();
					state = STATE_FALLING;
					Log.i(activitynametag, "FALLING! vectorLen() was " + vectorLen(x,y,z) + ", getOffsetAbs() was " + getOffsetAbs());
					
					falling_timeout = SystemClock.uptimeMillis();
				}
			}
		
			else if(state == STATE_FALLING && vectorLen(x,y,z) > getOffsetAbs()/2.0) {
				if(falling_timeout + MIN_TIME_BETWEEN_FALLS_MS < SystemClock.uptimeMillis()) {
					MessageRouter.sendFallingMessage();
					state = STATE_NOT_FALLING;
					Log.i(activitynametag, "NOT FALLING! vectorLen() was " + vectorLen(x,y,z) + ", getOffsetAbs() was " + getOffsetAbs());
					
					falling_timeout = SystemClock.uptimeMillis();
				}
			}
		
		} else {
			Log.v(activitynametag, (LAST_SAMPLE_DEPTH - last_samplecount) + " samples remaining until we can calculate motion");
			updateLastSamples(x,y,z);
			state = STATE_NOT_FALLING;
		}		
	}
	
}
