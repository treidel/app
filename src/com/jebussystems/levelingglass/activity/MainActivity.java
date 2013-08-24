package com.jebussystems.levelingglass.activity;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.Level;
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

	private LevelingGlassApplication application;
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

		// fetch the application
		this.application = (LevelingGlassApplication) getApplication();

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

		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();
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
		// clear all existing views
		this.layout.removeViews(0, this.layout.getChildCount());

		// explode the view layout
		LayoutInflater vi = (LayoutInflater) getApplicationContext()
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for (Map.Entry<Integer, Level> entry : application.getControl()
		        .getLevels().entrySet())
		{

			// this will be populated below
			int layoutId;
			switch (entry.getValue())
			{
				case NONE:
					layoutId = R.layout.nolevel;
					break;
				case PEAK:
					layoutId = R.layout.peaklevel;
					break;
				case VU:
					layoutId = R.layout.vulevel;
					break;
				default:
					Log.wtf(TAG, "unknown level type=" + entry.getValue());
					return;
			}
			// inflate the layout for the audio view
			ViewGroup viewGroup = (ViewGroup) vi.inflate(layoutId, null);
			// add to the layout
			this.layout.addView(viewGroup, entry.getKey() - 1);
			// find the audio view
			AudioLevelView view = (AudioLevelView) viewGroup
			        .findViewById(R.id.audio_view);
			// add a listener
			view.setOnClickListener(new LevelViewClickListener());
		}
	}

	private void updateLevelData()
	{
		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();
		// query the level data from the control object
		Map<Integer, LevelDataRecord> records = control.getLevelDataRecord();
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
						audiolevel.setLevel(((PeakLevelDataRecord) record)
						        .getPeakLevelInDB());
						break;
					case VU:
						// set the level
						audiolevel.setLevel(((VULevelDataRecord) record)
						        .getPowerLevelInDB());
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
			if (false == ControlV1.State.CONNECTED.equals(state))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						finish();
					}
				});
			}
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

	private class LevelViewClickListener implements View.OnClickListener
	{

		@Override
		public void onClick(View view)
		{
			// figure out which view was clicked on
			int index = layout.indexOfChild(view);
			// pop up the level dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(
			        MainActivity.this);
			LayoutInflater inflater = getLayoutInflater();
			ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.levelselection, null); 
			builder.setView(layout);
			builder.setPositiveButton(getString(android.R.string.ok),
			        new LevelRadioClickListener(layout, index));
			AlertDialog dialog = builder.create();
			dialog.show();
		}

	}

	private class LevelRadioClickListener implements
	        DialogInterface.OnClickListener
	{
		private final ViewGroup layout;
		private final int index;

		public LevelRadioClickListener(ViewGroup layout, int index)
		{
			this.layout = layout;
			this.index = index;
		}

		@Override
		public void onClick(DialogInterface dialog, int ignore)
		{
			// fetch the radio group
			RadioGroup group = (RadioGroup) layout.findViewById(R.id.radiogroup_level);

			// get the selected button
			int id = group.getCheckedRadioButtonId();
			switch (id)
			{
				case R.id.radio_levelselection_none:
					application.getControl().updateLevel(index, Level.NONE);
					break;
				case R.id.radio_levelsection_peak:
					application.getControl().updateLevel(index, Level.PEAK);
					break;
				case R.id.radio_levelsection_vu:
					application.getControl().updateLevel(index, Level.VU);
					break;
				default:
					Log.wtf(TAG, "unknown radio button id=" + id);
					return;
			}
			// force a recreation of all level views
			populateLevelViews();
		}
	}
}
