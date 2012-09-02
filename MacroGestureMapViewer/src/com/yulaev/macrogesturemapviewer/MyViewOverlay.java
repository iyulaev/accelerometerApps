/** Used to implement ontap */

package com.yulaev.macrogesturemapviewer;

import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MyViewOverlay extends Overlay {

    public MyViewOverlay() {
        super();
    }

    @Override
	public boolean onTap(GeoPoint p, MapView mapView) {
    	MessageRouter.toggleAccelerometerCalibration();
    	return(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me, MapView mapView) {
    	MessageRouter.toggleAccelerometerCalibration();
    	return super.onTouchEvent(me, mapView);
    }
}
