package com.yulaev.macrogesturemapviewer;

import android.os.Handler;
import android.os.Message;

public class MessageRouter {
	
	public static final int MESSAGE_ZOOM_CHANGED = 1;
	public static final int MESSAGE_PAN_CHANGED = 2;
	public static final int MESSAGE_TOGGLE_ACCELEROMETER_CALIBRATION = 3;
	
	public static Handler mainActivityHandler;
	public static Handler accelerometerThreadHandler;
	
	/*public static void sendZoomMessage(int relative_zoom_change) {
		
	}*/
	
	/** Sends message to MainActivity indicating that a pan action has occurred
	 * 
	 * @param x
	 * @param y
	 */
	public static void sendPanMessage (int x, int y) {
		if(mainActivityHandler != null) {
			Message message = Message.obtain();
			message.what = MESSAGE_PAN_CHANGED;
			message.arg1 = x;
			message.arg2 = y;
			mainActivityHandler.sendMessage(message);
		}
	}
	
	public static void toggleAccelerometerCalibration () {
		if(accelerometerThreadHandler != null) {
			Message message = Message.obtain();
			message.what = MESSAGE_TOGGLE_ACCELEROMETER_CALIBRATION;
			accelerometerThreadHandler.sendMessage(message);
		}
	}

}
