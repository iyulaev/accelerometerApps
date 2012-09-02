package com.yulaev.macrogesturemapviewer;

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
        
        //Not presently used		
		last_samples_x = new double[LAST_SAMPLE_DEPTH];
		last_samples_y = new double[LAST_SAMPLE_DEPTH];
		last_samples_z = new double[LAST_SAMPLE_DEPTH];
		last_samplecount = 0;
		last_update_millis = -1;
		
		velocity_x = 0.0d;
		velocity_y = 0.0d;
		velocity_z = 0.0d;
		displacement_x = 0.0d;
		displacement_y = 0.0d;
		displacement_z = 0.0d;
		
        
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
			MessageRouter.sendPanMessage((int)(velocity_x * 1000000), (int)(velocity_y * 1000000));
			
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
	private int last_samplecount;
	
	private void updateLastSamples(float x, float y, float z) {
		if(last_samplecount < LAST_SAMPLE_DEPTH) {
			last_samples_x[last_samplecount] = x;
			last_samples_y[last_samplecount] = y;
			last_samples_z[last_samplecount] = z;
			
			last_samplecount++;
		}
		
		else {
			for(int i = 0; i < LAST_SAMPLE_DEPTH-1; i++) {
				last_samples_x[i] = last_samples_x[i+1];
				last_samples_y[i] = last_samples_y[i+1];
				last_samples_z[i] = last_samples_z[i+1];
			}
			
			last_samples_x[LAST_SAMPLE_DEPTH-1] = x;
			last_samples_y[LAST_SAMPLE_DEPTH-1] = y;
			last_samples_z[LAST_SAMPLE_DEPTH-1] = z;
		}
	}
	
	private double getOffsetX() {
		if(last_samplecount < LAST_SAMPLE_DEPTH) return(0.0d);
		else {
			double retval = 0.0f;
			for(int i = 0; i < LAST_SAMPLE_DEPTH-1; i++)
				retval += last_samples_x[i];
			retval /= last_samplecount;
			return(retval);
		}
	}
	
	private double getOffsetY() {
		if(last_samplecount < LAST_SAMPLE_DEPTH) return(0.0d);
		else {
			double retval = 0.0f;
			for(int i = 0; i < LAST_SAMPLE_DEPTH-1; i++)
				retval += last_samples_y[i];
			retval /= last_samplecount;
			return(retval);
		}
	}
	
	private double getOffsetZ() {
		if(last_samplecount < LAST_SAMPLE_DEPTH) return(0.0d);
		else {
			double retval = 0.0f;
			for(int i = 0; i < LAST_SAMPLE_DEPTH-1; i++)
				retval += last_samples_z[i];
			retval /= last_samplecount;
			return(retval);
		}
	}
	
	private final float NOISE = 0.25f;
	double velocity_x, velocity_y, velocity_z;
	double displacement_x, displacement_y, displacement_z;
	long last_update_millis;
	
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		
		updateLastSamples(x,y,z);
		
		
		if(last_samplecount >= LAST_SAMPLE_DEPTH) {
			/*Log.v(activitynametag, "ACCELEROMETER AC COUPLED (x,y,z) = (" + (x-getOffsetX()) + 
					", " + (y-getOffsetY()) + ", " + (z-getOffsetZ()) + ")");*/
			
			x -= getOffsetX();
			y -= getOffsetY();
			z -= getOffsetZ();
			
			if(last_update_millis > 0) {
				if(Math.abs(x) > NOISE) {
					velocity_x += (SystemClock.uptimeMillis() - last_update_millis) * x;
					displacement_x += velocity_x * (SystemClock.uptimeMillis() - last_update_millis);
				}
				if(Math.abs(y) > NOISE) {
					velocity_y += (SystemClock.uptimeMillis() - last_update_millis) * y;
					displacement_y += velocity_y * (SystemClock.uptimeMillis() - last_update_millis);
				}
				if(Math.abs(z) > NOISE) {
					velocity_z += (SystemClock.uptimeMillis() - last_update_millis) * z;
					displacement_z += velocity_z * (SystemClock.uptimeMillis() - last_update_millis);
				}
			
			}
				
			last_update_millis = SystemClock.uptimeMillis();
		} else {
			Log.v(activitynametag, (LAST_SAMPLE_DEPTH - last_samplecount) + " samples remaining until we can calculate motion");
			
			velocity_x = 0;
			velocity_y = 0;
			velocity_z = 0;
		}		
	}
	
}
