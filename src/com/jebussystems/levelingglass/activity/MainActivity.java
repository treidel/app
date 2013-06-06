package com.jebussystems.levelingglass.activity;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.ControlV1.LevelDataRecord;
import com.jebussystems.levelingglass.view.AudioLevelView;

public class MainActivity extends Activity
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "activity.main";

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private ControlEventListener listener = new ControlEventListener();
	private ViewGroup layout = null;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.i(TAG, "onCreate");

		// setup the layout
		setContentView(R.layout.main);

		// find the layout
		this.layout = (ViewGroup) findViewById(R.id.main_layout);
	}

	@Override
	protected void onStart()
	{
		// call the base class
		super.onStart();

		Log.i(TAG, "onStart");

		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();

		// get the control object
		ControlV1 control = application.getControl();
		// get the control object and add ourselves as a listener
		control.addListener(listener);

		// see if we're ready to go
		if (null == control.getChannels())
		{
			// not ready to display levels - switch to the waiting activity
			Intent intent = new Intent(this, WaitingForConnectionActivity.class);
			startActivity(intent);
			// done
			return;
		}

		// do the layout
		populateLevelViews();

		// try the populate the level views
		updateLevelData();
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		Log.i(TAG, "onStop");

		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();

		// get the control object and add ourselves as a listener
		application.getControl().removeListener(listener);
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private void populateLevelViews()
	{
		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();
		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();

		for (int level : control.getChannels())
		{
			// explode the view layout
			LayoutInflater vi = (LayoutInflater) getApplicationContext()
			        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = vi.inflate(R.layout.level, null);
			// insert into main view
			layout.addView(view, level - 1, new ViewGroup.LayoutParams(
			        ViewGroup.LayoutParams.MATCH_PARENT,
			        ViewGroup.LayoutParams.MATCH_PARENT));
		}
	}

	private void updateLevelData()
	{
		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();
		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();
		// query the level data from the control object
		Map<Integer, ControlV1.LevelDataRecord> records = control
		        .getLevelDataRecord();
		if (null != records)
		{
			for (LevelDataRecord record : records.values())
			{
				// find the layout view
				View view = layout.getChildAt(record.getChannel() - 1);
				// find the audio level view
				AudioLevelView audiolevel = (AudioLevelView) view
				        .findViewById(R.id.audio_view);
				// set the level
				audiolevel.setLevel(record.getLevelInDB());
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ControlEventListener implements ControlV1.EventListener
	{
		@Override
		public void notifyConnected()
		{
			Log.w(TAG, "unexpected notifyConnected event");
		}

		@Override
		public void notifyLevelsUpdated()
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// run the common update logic
					updateLevelData();
				}
			});

		}

	}

}
