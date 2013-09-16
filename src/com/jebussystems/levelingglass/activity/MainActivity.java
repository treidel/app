package com.jebussystems.levelingglass.activity;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.DigitalPeakLevelDataRecord;
import com.jebussystems.levelingglass.control.LevelDataRecord;
import com.jebussystems.levelingglass.control.MeterConfig;
import com.jebussystems.levelingglass.control.MeterType;
import com.jebussystems.levelingglass.control.PPMLevelDataRecord;
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
	private ControlEventListener listener;
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
		Log.v(TAG, "MainActivity::onCreate enter savedInstanceState="
		        + savedInstanceState);

		super.onCreate(savedInstanceState);

		// fetch the application
		this.application = (LevelingGlassApplication) getApplication();

		// create the listener
		this.listener = new ControlEventListener();

		// start a connection to the device
		BluetoothDevice device = application.getDevice();
		application.getControl().getManager().connect(device);

		// setup the layout
		setContentView(R.layout.main);

		// find the layout
		this.layout = (ViewGroup) findViewById(R.id.main_layout);

		Log.v(TAG, "MainActivity::onCreate exit");

	}

	@Override
	protected void onStart()
	{
		Log.v(TAG, "MainActivity::onStart enter");

		// call the base class
		super.onStart();

		// get the control object and add ourselves as a listener
		ControlV1 control = application.getControl();
		control.addListener(listener);

		// refresh the dialog state
		listener.refresh();

		// do the layout
		populateLevelViews();

		// try the populate the level views
		updateLevelData();

		Log.v(TAG, "MainActivity::onStart exit");
	}

	@Override
	protected void onStop()
	{
		Log.v(TAG, "MainActivity::onStop enter");

		super.onStop();

		// get the control object and add ourselves as a listener
		application.getControl().removeListener(listener);

		Log.v(TAG, "MainActivity::onStop exit");
	}

	@Override
	protected void onDestroy()
	{
		Log.v(TAG, "MainActivity::onDestroy enter");

		super.onStop();

		// see if this is a real destroy
		if (true == isFinishing())
		{
			// force a disconnect
			application.getControl().getManager().disconnect();
		}

		Log.v(TAG, "MainActivity::onDestroy exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private void populateLevelViews()
	{
		// clear all existing views
		this.layout.removeAllViews();

		// explode the view layout
		LayoutInflater vi = (LayoutInflater) getApplicationContext()
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		for (int channel : application.getChannelSet())
		{
			// get the channel config
			MeterConfig config = application.getConfigForChannel(channel);
			// this will be populated below
			int layoutId;
			switch (config.getMeterType())
			{
				case NONE:
					layoutId = R.layout.nolevel;
					break;
				case DIGITALPEAK:
					layoutId = R.layout.digitalpeaklevel;
					break;
				case PPM:
					layoutId = R.layout.ppmlevel;
					break;
				case VU:
					layoutId = R.layout.vulevel;
					break;
				default:
					Log.wtf(TAG, "unknown level type=" + config.getMeterType());
					return;
			}
			// inflate the layout for the audio view
			ViewGroup viewGroup = (ViewGroup) vi.inflate(layoutId, null);
			// find the display view
			View view = viewGroup.findViewById(R.id.audio_view);
			// remove it from the exploded layout
			viewGroup.removeView(view);
			// add the display view to the layout
			this.layout.addView(view, channel - 1);
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
					case PPM:
						// set the level
						audiolevel.setLevel(((PPMLevelDataRecord) record)
						        .getPeakLevelInDB());
						break;
					case DIGITALPEAK:
						// set the level
						audiolevel
						        .setLevel(((DigitalPeakLevelDataRecord) record)
						                .getPeakLevelInDB());
						break;
					case VU:
						// set the level
						audiolevel.setLevel(((VULevelDataRecord) record)
						        .getVUInUnits());
						break;
					default:
						Log.wtf(TAG, "unexpected level=" + record.getType());
						return;
				}

			}
		}
	}

	private void updateSelectionDialog(ViewGroup layout, int checkedId)
	{
		// find the seek bar
		SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seekbar_holdtime);

		switch (checkedId)
		{
			case R.id.radio_levelselection_none:
			case R.id.radio_levelsection_vu:
				// they selected none or VU so hide the seek bar
				seekBar.setVisibility(View.INVISIBLE);
				break;

			case R.id.radio_levelsection_digitalpeak:
			case R.id.radio_levelsection_ppm:
				// they selected digital or PPM so reveal the seek bar
				seekBar.setVisibility(View.VISIBLE);
				break;
			default:
				Log.wtf(TAG, "unknown radio button id=" + checkedId);
				return;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ControlEventListener implements ControlV1.EventListener
	{
		private final AlertDialog dialog;
		private final TextView statusView;

		public ControlEventListener()
		{
			// create the builder
			AlertDialog.Builder builder = new AlertDialog.Builder(
			        MainActivity.this);
			LayoutInflater inflater = getLayoutInflater();
			ViewGroup layout = (ViewGroup) inflater.inflate(
			        R.layout.waitingforconnection, null);
			// save a reference to the status textview
			this.statusView = (TextView) layout
			        .findViewById(R.id.status_textview);
			// update the peer name
			TextView peerView = (TextView) layout
			        .findViewById(R.id.peer_textview);
			peerView.setText(application.getDevice().toString());
			// set the layout for the dialog
			builder.setView(layout);
			builder.setPositiveButton(android.R.string.cancel,
			        new CancelConnectionClickListener());
			builder.setCancelable(true);
			// create + run the dialog
			this.dialog = builder.create();
		}

		public void refresh()
		{
			final ControlV1.State state = application.getControl().getState();
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (true == state.equals(ControlV1.State.CONNECTED))
					{
						dialog.hide();
					}
					else
					{
						statusView.setText(state.toString());
						dialog.show();
					}
				}
			});
		}

		@Override
		public void notifyStateChange(ControlV1.State state)
		{
			Log.v(TAG,
			        "MainActivity::ControlEventListener::notifyStateChange enter state="
			                + state);
			final ControlV1.State copy = state;
			if (false == ControlV1.State.CONNECTED.equals(state))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// update the text
						statusView.setText(copy.toString());
						// show the dialog
						dialog.show();
					}
				});
			}
			else
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// hide the dialog
						dialog.hide();
					}
				});
			}
			Log.v(TAG,
			        "MainActivity::ControlEventListener::notifyStateChange exit");
		}

		@Override
		public void notifyLevelsUpdated()
		{
			Log.v(TAG,
			        "MainActivity::ControlEventListener::notifyLevelsUpdated enter");
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					Log.v(TAG,
					        "MainActivity::ControlEventListener::notifyLevelsUpdated::run enter");
					// run the common update logic
					updateLevelData();
					Log.v(TAG,
					        "MainActivity::ControlEventListener::notifyLevelsUpdated::run exit");
				}
			});
			Log.v(TAG,
			        "MainActivity::ControlEventListener::notifyLevelsUpdated exit");
		}
	}

	private class LevelViewClickListener implements View.OnClickListener
	{

		@Override
		public void onClick(View view)
		{
			Log.v(TAG,
			        "MainActivity::LevelViewClickListener::onClick::run enter view="
			                + view);

			// figure out which view was clicked on
			int index = layout.indexOfChild(view);
			// pop up the level dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(
			        MainActivity.this);
			LayoutInflater inflater = getLayoutInflater();
			ViewGroup layout = (ViewGroup) inflater.inflate(
			        R.layout.levelselection, null);
			// populate which level we're currently set to
			RadioGroup radioGroup = (RadioGroup) layout
			        .findViewById(R.id.radiogroup_level);
			MeterConfig config = application.getConfigForChannel(index + 1);
			switch (config.getMeterType())
			{
				case NONE:
					radioGroup.check(R.id.radio_levelselection_none);
					break;
				case PPM:
					radioGroup.check(R.id.radio_levelsection_ppm);
					break;
				case DIGITALPEAK:
					radioGroup.check(R.id.radio_levelsection_digitalpeak);
					break;
				case VU:
					radioGroup.check(R.id.radio_levelsection_vu);
					break;
				default:
					Log.wtf(TAG, "unknown level=" + config.getMeterType());
					return;
			}
			// add a listener
			radioGroup
			        .setOnCheckedChangeListener(new LevelRadioCheckedChangeListener(
			                layout));
			// update the dialog
			updateSelectionDialog(layout, radioGroup.getCheckedRadioButtonId());
			// finish popping up the dialog
			builder.setView(layout);
			builder.setPositiveButton(getString(android.R.string.ok),
			        new LevelRadioClickListener(layout, config.getChannel()));
			AlertDialog dialog = builder.create();
			dialog.show();

			Log.v(TAG,
			        "MainActivity::LevelViewClickListener::onClick::run exit");
		}
	}

	private class LevelRadioClickListener implements
	        DialogInterface.OnClickListener
	{
		private final ViewGroup layout;
		private final int channel;

		public LevelRadioClickListener(ViewGroup layout, int channel)
		{
			this.layout = layout;
			this.channel = channel;
		}

		@Override
		public void onClick(DialogInterface dialog, int ignore)
		{
			Log.v(TAG,
			        "MainActivity::LevelRadioClickListener::onClick::run enter dialog="
			                + dialog + " ignore=" + ignore);

			// fetch the radio group
			RadioGroup group = (RadioGroup) layout
			        .findViewById(R.id.radiogroup_level);

			// get the selected button
			int id = group.getCheckedRadioButtonId();
			// convert to a type
			MeterType type = convertRadioIdToMeterType(id);
			// assume no hold time for now
			Integer holdtime = null;
			// for certain meter types we have one
			switch (type)
			{
				case DIGITALPEAK:
				case PPM:
				{
					// extract the hold time
					SeekBar seekBar = (SeekBar) layout
					        .findViewById(R.id.seekbar_holdtime);
					holdtime = seekBar.getProgress();
				}
					break;
				default:
					Log.v(TAG, "no hold time needed for type=" + type);
					break;
			}

			// create the meter config object
			MeterConfig config = new MeterConfig(channel, type);
			config.setHoldtime(holdtime);

			// store the meter config
			application.setConfigForChannel(config);

			// set the config
			application.getControl().notifyLevelConfigChange();

			// force a recreation of all level views
			populateLevelViews();

			Log.v(TAG,
			        "MainActivity::LevelRadioClickListener::onClick::run exit");
		}

		private MeterType convertRadioIdToMeterType(int id)
		{
			switch (id)
			{
				case R.id.radio_levelselection_none:
					return MeterType.NONE;
				case R.id.radio_levelsection_digitalpeak:
					return MeterType.DIGITALPEAK;
				case R.id.radio_levelsection_ppm:
					return MeterType.PPM;
				case R.id.radio_levelsection_vu:
					return MeterType.VU;
				default:
					Log.wtf(TAG, "unknown radio button id=" + id);
					return null;
			}
		}
	}

	private class LevelRadioCheckedChangeListener implements
	        OnCheckedChangeListener
	{
		private final ViewGroup layout;

		public LevelRadioCheckedChangeListener(ViewGroup layout)
		{
			this.layout = layout;
		}

		@Override
		public void onCheckedChanged(RadioGroup group, int id)
		{
			Log.v(TAG,
			        "MainActivity::LevelRadioCheckedChangeListener::onCheckedChanged enter group"
			                + group + " id=" + id);
			// update the selection dialog
			updateSelectionDialog(layout, id);
			Log.v(TAG,
			        "MainActivity::LevelRadioCheckedChangeListener::onCheckedChanged exit");
		}

	}

	private class CancelConnectionClickListener implements
	        DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			Log.v(TAG,
			        "MainActivity::CancelConnectionClickListener::onClick enter dialog="
			                + dialog + " which=" + which);
			// disconnect and close this activity
			application.getControl().getManager().disconnect();
			MainActivity.this.finish();
			Log.v(TAG,
			        "MainActivity::CancelConnectionClickListener::onClick exit");
		}

	}
}
