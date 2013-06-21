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
import com.jebussystems.levelingglass.control.LevelDataRecord;
import com.jebussystems.levelingglass.control.PeakLevelDataRecord;
import com.jebussystems.levelingglass.control.VULevelDataRecord;
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

	private final LevelingGlassApplication application = (LevelingGlassApplication)getApplication();
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

		// get the control object
		ControlV1 control = application.getControl();

		// see if we're ready to go
		if (null == control.getChannels())
		{
			// not ready to display levels - switch to the waiting activity
			Intent intent = new Intent(this, WaitingForConnectionActivity.class);
			startActivity(intent);
			// done
			return;
		}
		
		// add ourselves as a listener
		control.addListener(listener);

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

		// get the control object and add ourselves as a listener
		application.getControl().removeListener(listener);
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private void populateLevelViews()
	{
		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();

		for (int level : control.getChannels())
		{
			// explode the view layout
			LayoutInflater vi = (LayoutInflater) getApplicationContext()
			        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = vi.inflate(R.layout.peaklevel, null);
			// insert into main view
			layout.addView(view, level - 1, new ViewGroup.LayoutParams(
			        ViewGroup.LayoutParams.MATCH_PARENT,
			        ViewGroup.LayoutParams.MATCH_PARENT));
		}
	}

	private void updateLevelData()
	{
		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();
		// query the level data from the control object
		Map<Integer, LevelDataRecord> records = control
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
				switch (record.getType())
				{
					case PEAK:
						// set the level
						audiolevel.setLevel(((PeakLevelDataRecord)record).getPeakLevelInDB());
						break;
					case VU:
						// set the level
						audiolevel.setLevel(((VULevelDataRecord)record).getPowerLevelInDB());
						break;
					default:
						Log.wtf(TAG, "unexpected level=" + record.getType());
						return;
				}

			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ControlEventListener implements ControlV1.EventListener
	{
		@Override
		public void notifyStateChange(ControlV1.State state)
		{
			Log.w(TAG, "ignore");
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
