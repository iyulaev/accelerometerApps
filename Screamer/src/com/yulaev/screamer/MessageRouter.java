package com.yulaev.screamer;

import android.os.Handler;
import android.os.Message;

public class MessageRouter {
	
	public static final int MESSAGE_ZOOM_CHANGED = 1;
	public static final int MESSAGE_FALLING = 2;
	public static final int MESSAGE_TOGGLE_ACCELEROMETER_CALIBRATION = 3;
	public static final int MESSAGE_CALIBRATION_INPROGRESS = 4;
	public static final int MESSAGE_CALIBRATION_DONE = 5;
	
	public static Handler mainActivityHandler;
	public static Handler accelerometerThreadHandler;
	
	/*public static void sendZoomMessage(int relative_zoom_change) {
		
	}*/
	
	public static void sendCalibratingMessage (boolean calibrating) {
		if(mainActivityHandler != null) {
			Message message = Message.obtain();
			message.what = calibrating ? MESSAGE_CALIBRATION_INPROGRESS : MESSAGE_CALIBRATION_DONE;
			mainActivityHandler.sendMessage(message);
		}
	}
	
	/** Sends message to MainActivity indicating that a pan action has occurred
	 * 
	 * @param x
	 * @param y
	 */
	public static void sendFallingMessage () {
		if(mainActivityHandler != null) {
			Message message = Message.obtain();
			message.what = MESSAGE_FALLING;
			mainActivityHandler.sendMessage(message);
		}
	}
	
	public static void accelerometerReset () {
		if(accelerometerThreadHandler != null) {
			Message message = Message.obtain();
			message.what = MESSAGE_TOGGLE_ACCELEROMETER_CALIBRATION;
			accelerometerThreadHandler.sendMessage(message);
		}
	}

}
