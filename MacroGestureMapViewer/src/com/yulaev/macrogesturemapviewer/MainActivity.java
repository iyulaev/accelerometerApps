package com.yulaev.macrogesturemapviewer;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.Overlay;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import android.hardware.SensorManager;

public class MainActivity extends MapActivity {
	
	final String activitynametag = "MainActivity";
	
	Handler messageHandler;
	AccelerometerThread accelerometerThread;
	
	MyView mapView;
	MapController myMapController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        mapView = (MyView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);
        mapView.getOverlays().add(new MyViewOverlay());
        
        //Start the map displaying San Jose (-ish)
        int startingLat = 37336000;
        int startingLon = -121893000;
        int startingZoom = 11;
        
        //Use the numbers we acquired above to set starting position and zoom level
		myMapController = mapView.getController();
		myMapController.setCenter(new GeoPoint(startingLat, startingLon));
		myMapController.setZoom(startingZoom);
		
		messageHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == MessageRouter.MESSAGE_ZOOM_CHANGED) {
					Log.d(activitynametag, "Got zoom message!");
				}
				
				else if (msg.what == MessageRouter.MESSAGE_PAN_CHANGED) {
					int x = msg.arg1;
					int y = msg.arg2;
					
					Log.d(activitynametag, "Got pan message! Params=(" + x + ", " + y + ")");
					
					x /= 50000000; x *= -1;
					y /= 50000000;
					myMapController.scrollBy(x,y);
				}
			}
		};
		
		MessageRouter.mainActivityHandler = messageHandler;
		
		accelerometerThread = new AccelerometerThread((SensorManager) getSystemService(Context.SENSOR_SERVICE));
		accelerometerThread.start();
		
		Button calibrateButton = (Button) findViewById(R.id.calibrate);
		calibrateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	MessageRouter.toggleAccelerometerCalibration();
            }
        }); 
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return true;
    }
	@Override
    protected boolean isLocationDisplayed() {
        return true;
    }
	
	@Override
	public void onDestroy() {
		accelerometerThread.terminate();
		
		super.onDestroy();
	}
}
