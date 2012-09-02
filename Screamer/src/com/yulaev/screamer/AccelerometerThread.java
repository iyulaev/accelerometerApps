package com.yulaev.screamer;

import android.content.Context;
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
        	
		last_samples_x = new double[LAST_SAMPLE_DEPTH];
		last_samples_y = new double[LAST_SAMPLE_DEPTH];
		last_samples_z = new double[LAST_SAMPLE_DEPTH];
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
	
	/** Does nothing! Everything is handled in the handler.
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
	private double [] last_samples_x;
	private double [] last_samples_y;
	private double [] last_samples_z;
	private double [] last_samples_abs;
	private int last_samplecount;
		
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
	
	private float vectorLen(float x, float y, float z) {
		return ( (float) Math.sqrt(Math.pow(x,2.0) + Math.pow(y,2.0) + Math.pow(z,2.0)) );
	}
	
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
