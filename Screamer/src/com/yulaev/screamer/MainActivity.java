/** The main activity class for Screamer. It instantiates the AccelerometerThread, the PointCounter,
 * and deals with updating all of the UI elements as well as playing sounds.
 * 
 * Maybe it would make sense to delegate sound playing to another Thread? Not sure.
 */

package com.yulaev.screamer;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.yulaev.screamer.R;


import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.hardware.SensorManager;

public class MainActivity extends Activity {
	
	final String activitynametag = "MainActivity";
	
	Handler messageHandler;
	AccelerometerThread accelerometerThread;
	
	//Used for keeping track of points
	PointCounter pointCounter;
	
	//Enum for display states
	final int DISPLAY_FALLING = 1;
	final int DISPLAY_CALIBRATING = 2;
	final int DISPLAY_READY = 3;
	
	RelativeLayout mainLayout;
	TextView textView;
	TextView throwScoreView;
	TextView gameScoreView;
	TextView throwHiScoreView;
	TextView gameHiScoreView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mainLayout = (RelativeLayout) findViewById(R.id.mainlayout);
        textView = (TextView) findViewById(R.id.hello);
        throwScoreView = (TextView) findViewById(R.id.throwscore);
        gameScoreView = (TextView) findViewById(R.id.gamescore);
        throwHiScoreView = (TextView) findViewById(R.id.throwhiscore);
        gameHiScoreView = (TextView) findViewById(R.id.gamehiscore);
        
        pointCounter = new PointCounter((Context)this, true);
        
        displayStateCalibrating();
        setupSounds();

		messageHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				
				if (msg.what == MessageRouter.MESSAGE_FALLING) {
					toggleDisplay(DISPLAY_FALLING);
				}
				else if (msg.what == MessageRouter.MESSAGE_CALIBRATION_INPROGRESS) {
					toggleDisplay(DISPLAY_CALIBRATING);
				}
				else if (msg.what == MessageRouter.MESSAGE_CALIBRATION_DONE) {
					toggleDisplay(DISPLAY_READY);
				}
			}
		};
		
		MessageRouter.mainActivityHandler = messageHandler;
		
		accelerometerThread = new AccelerometerThread((SensorManager) getSystemService(Context.SENSOR_SERVICE));
		accelerometerThread.start();
		MessageRouter.accelerometerReset();
		
		Button calibrateButton = (Button) findViewById(R.id.calibrate);
		calibrateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	MessageRouter.accelerometerReset();
            	pointCounter.reset();
            }
        }); 
    }
    
    @Override
	public void onDestroy() {
		accelerometerThread.terminate();
		pointCounter.saveScores(this);
		super.onDestroy();
	}
    
    
    
    
    
    private int display_state;
    private long toss_duration;
    /** Advance the display state machine based on the last input that we've received, typically from
     * the AccelerometerThread.
     * @param display_input The last input signal received.
     */
    private void toggleDisplay(int display_input) {   	
    	if(display_input == DISPLAY_CALIBRATING) {
    		playNoSound();
    		displayStateCalibrating();
    	}
    	else if (display_input == DISPLAY_FALLING) {
    		//If we were previously falling, go to not falling
    		if(display_state == DISPLAY_FALLING) {
    			playThudSound();
    			toss_duration = SystemClock.uptimeMillis() - toss_duration;
    			pointCounter.processThrowScore(calculateThrowScore(toss_duration));
    			displayStateReady();
    			
    		}
    		//Else, if we were previously NOT falling, now we are!
    		else {
    			playScreamSound();
    			toss_duration = SystemClock.uptimeMillis();
    			displayStateFalling();
    		}
    	}
    	else if (display_input == DISPLAY_READY) {
    		if(display_state == DISPLAY_CALIBRATING)
    			displayStateReady();
    	}
    }
    
    private void displayStateCalibrating() {
    	display_state = DISPLAY_CALIBRATING;
    	
    	mainLayout.setBackgroundColor(0xFF222222);
    	textView.setText("Calibrating...");
    	updateScores();
    }
    
    private void displayStateFalling() {
    	display_state = DISPLAY_FALLING;
    	
    	mainLayout.setBackgroundColor(0xFFFF0000);
    	textView.setText("Aaaahhhhhhh!");
    	updateScores();
    }
    
    private void displayStateReady() {
    	display_state = DISPLAY_READY;
    	
    	mainLayout.setBackgroundColor(0xFF00CC00);
    	textView.setText("Ready!");
    	updateScores();
    }
    
    private void updateScores() {
    	throwScoreView.setText("Last Throw: " + Integer.toString(pointCounter.getLastThrowPoints()));
    	gameScoreView.setText("Game Score:" + Integer.toString(pointCounter.getThisGamePoints()));
    	throwHiScoreView.setText("Best Throw: " + Integer.toString(pointCounter.getThrowHiScore()));
    	gameHiScoreView.setText("Game Hi Score: " + Integer.toString(pointCounter.getGameHiScore()));
    }
    
    private SoundPool mSoundPool;
    private AudioManager  mAudioManager;
    private HashMap<Integer, Integer> mSoundPoolMap;

    private int mStream = 0;
    
    final int SOUND_SCREAM = 1;
	final int SOUND_THUD = 2;
    private void setupSounds() {
    	mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
    	mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

    	mSoundPoolMap = new HashMap<Integer, Integer>();
    	mSoundPoolMap.put(SOUND_SCREAM, mSoundPool.load(this, R.raw.scream, 1));
    	mSoundPoolMap.put(SOUND_THUD, mSoundPool.load(this, R.raw.thud, 1));
    }
    
    private void playScreamSound() {
    	mSoundPool.stop(mStream);
    	float streamVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    	mStream = mSoundPool.play(mSoundPoolMap.get(SOUND_SCREAM), streamVolume, streamVolume, 1, 0, 1f);
    	
    	Log.v(activitynametag, "Sound: SCREAM!");
    }
    
    private void playThudSound() {
    	mSoundPool.stop(mStream);
    	float streamVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    	mStream = mSoundPool.play(mSoundPoolMap.get(SOUND_THUD), streamVolume, streamVolume, 1, 0, 1f);
    	
    	Log.v(activitynametag, "Sound: THUD!");
    }
    
    private void playNoSound() {
    	mSoundPool.stop(mStream);
    	
    	Log.v(activitynametag, "Sound: off");
    }
    
    /** Calculates the number of points aquired for a throw lasting throw_duration ms.
     * 
     * @param throw_duration
     * @return
     */
    private int calculateThrowScore(long throw_duration) {
    	Random rand = new Random();
    	
    	return( (int) (((float)throw_duration) * 3 * (rand.nextFloat()+1.0)) );
    }
	
}
