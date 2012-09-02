package com.yulaev.screamer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;

public class PointCounter implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int last_throw_points;
	private int this_game_points;
	private int throw_hiscore_points;
	private int game_hiscore_points;
	
	private static transient int number_of_instances = 0;
	private transient int this_instance;
	
	public PointCounter(Context context, boolean do_reset_instance_count) {
		last_throw_points = 0;
		this_game_points = 0;
		
		//Change below to load from file?
		throw_hiscore_points = 0;
		game_hiscore_points = 0;
		
		if(do_reset_instance_count) resetInstanceCount();
		this_instance = number_of_instances++;
		
		//Try loading saved data from file
		String fileName = "pointcounter_" + Integer.toString(this_instance);
		PointCounter loaded = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		
		try {
			fis = context.openFileInput(fileName);
			in = new ObjectInputStream(fis);
			loaded = (PointCounter) in.readObject();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		if (loaded!=null) {
			this.last_throw_points = loaded.getLastThrowPoints();
			this.this_game_points = loaded.getThisGamePoints();
			this.throw_hiscore_points = loaded.getThrowHiScore();
			this.game_hiscore_points = loaded.getGameHiScore();
		}
		
	}
	
	public void reset() {
		last_throw_points = 0;
		this_game_points = 0;
	}
	
	private void resetInstanceCount() {
		number_of_instances = 0;
		this_instance = number_of_instances++;
	}
	
	public void processThrowScore (int n_throw_points) {
		last_throw_points = n_throw_points;
		this_game_points += n_throw_points;
		
		if(game_hiscore_points < this_game_points)
			game_hiscore_points = this_game_points;

		if(throw_hiscore_points < n_throw_points)
			throw_hiscore_points = n_throw_points;
	}
	
	public int getLastThrowPoints() { return last_throw_points; }
	public int getThisGamePoints() { return this_game_points; }
	public int getThrowHiScore() { return throw_hiscore_points; }
	public int getGameHiScore() { return game_hiscore_points; }
	
	public void saveScores(Context context) {
		String fileName = "pointcounter_" + Integer.toString(this_instance);
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			context.deleteFile(fileName);
			fos = context.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
