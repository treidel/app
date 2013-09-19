package com.jebussystems.levelingglass.activity;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.LevelDataRecord;
import com.jebussystems.levelingglass.control.MeterConfig;
import com.jebussystems.levelingglass.control.MeterType;
import com.jebussystems.levelingglass.control.PeakLevelDataRecord;
import com.jebussystems.levelingglass.control.VULevelDataRecord;
import com.jebussystems.levelingglass.util.LogWrapper;
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
		LogWrapper.v(TAG, "MainActivity::onCreate enter", "this=", this,
		        "savedInstanceState=", savedInstanceState);

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

		LogWrapper.v(TAG, "MainActivity::onCreate exit");

	}

	@Override
	protected void onStart()
	{
		LogWrapper.v(TAG, "MainActivity::onStart enter", "this=", this);

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

		LogWrapper.v(TAG, "MainActivity::onStart exit");
	}

	@Override
	protected void onStop()
	{
		LogWrapper.v(TAG, "MainActivity::onStop enter", "this=", this);

		super.onStop();

		// get the control object and add ourselves as a listener
		application.getControl().removeListener(listener);

		LogWrapper.v(TAG, "MainActivity::onStop exit");
	}

	@Override
	protected void onDestroy()
	{
		LogWrapper.v(TAG, "MainActivity::onDestroy enter");

		super.onStop();

		// clean up the listener
		listener.stop();

		// see if this is a real destroy
		if (true == isFinishing())
		{
			// force a disconnect
			application.getControl().getManager().disconnect();
		}

		LogWrapper.v(TAG, "MainActivity::onDestroy exit");
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
					LogWrapper.wtf(TAG, "unknown level type=",
					        config.getMeterType());
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
					case DIGITALPEAK:
						// set the level
						audiolevel.setLevel(((PeakLevelDataRecord) record)
						        .getPeakLevelInDB());
						audiolevel.setHold(((PeakLevelDataRecord) record)
						        .getHoldLevelInDB());
						break;
					case VU:
						// set the level
						audiolevel.setLevel(((VULevelDataRecord) record)
						        .getVUInUnits());
						break;
					default:
						LogWrapper.wtf(TAG, "unexpected level=",
						        record.getType());
						return;
				}

			}
		}
	}

	private void updateSelectionDialog(ViewGroup layout, int checkedId)
	{
		// find the hold time layout
		View holdtimeLayout = layout.findViewById(R.id.layout_holdtime);

		switch (checkedId)
		{
			case R.id.radio_levelselection_none:
			case R.id.radio_levelsection_vu:
				// they selected none or VU so hide the hold time views
				holdtimeLayout.setVisibility(View.INVISIBLE);
				break;

			case R.id.radio_levelsection_digitalpeak:
			case R.id.radio_levelsection_ppm:
				// they selected digital or PPM so reveal the hold time views
				holdtimeLayout.setVisibility(View.VISIBLE);
				break;
			default:
				LogWrapper.wtf(TAG, "unknown radio button id=", checkedId);
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

		public void stop()
		{
			this.dialog.dismiss();
		}

		@Override
		public void notifyStateChange(ControlV1.State state)
		{
			LogWrapper
			        .v(TAG,
			                "MainActivity::ControlEventListener::notifyStateChange enter",
			                "this=", this, "state=", state);
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
			LogWrapper
			        .v(TAG,
			                "MainActivity::ControlEventListener::notifyStateChange exit");
		}

		@Override
		public void notifyLevelsUpdated()
		{
			LogWrapper
			        .v(TAG,
			                "MainActivity::ControlEventListener::notifyLevelsUpdated enter",
			                "this=", this);
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					LogWrapper
					        .v(TAG,
					                "MainActivity::ControlEventListener::notifyLevelsUpdated::run enter",
					                "this=", this);
					// run the common update logic
					updateLevelData();
					LogWrapper
					        .v(TAG,
					                "MainActivity::ControlEventListener::notifyLevelsUpdated::run exit");
				}
			});
			LogWrapper
			        .v(TAG,
			                "MainActivity::ControlEventListener::notifyLevelsUpdated exit");
		}
	}

	private class LevelViewClickListener implements View.OnClickListener
	{

		@Override
		public void onClick(View view)
		{
			LogWrapper.v(TAG,
			        "MainActivity::LevelViewClickListener::onClick::run enter",
			        "this=", this, "view=", view);

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
					LogWrapper
					        .wtf(TAG, "unknown level=", config.getMeterType());
					return;
			}
			// add a listener
			radioGroup
			        .setOnCheckedChangeListener(new LevelRadioCheckedChangeListener(
			                layout));
			// update the dialog
			updateSelectionDialog(layout, radioGroup.getCheckedRadioButtonId());
			// add a listener for the hold time seekbar
			SeekBar holdtimeSeekbar = (SeekBar) layout
			        .findViewById(R.id.seekbar_holdtime);
			TextView holdtimeTextview = (TextView) layout
			        .findViewById(R.id.textview_holdtime);
			holdtimeSeekbar
			        .setOnSeekBarChangeListener(new HoldTimeSeekbarChangeListener(
			                holdtimeTextview));
			if (null != config.getHoldtime())
			{
				holdtimeSeekbar.setProgress(config.getHoldtime());
			}
			// finish popping up the dialog
			builder.setView(layout);
			builder.setPositiveButton(getString(android.R.string.ok),
			        new LevelRadioClickListener(layout, config.getChannel()));
			AlertDialog dialog = builder.create();
			dialog.show();

			LogWrapper.v(TAG,
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
			LogWrapper
			        .v(TAG,
			                "MainActivity::LevelRadioClickListener::onClick::run enter",
			                "this=", this, "dialog=", dialog, "ignore=", ignore);

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
					LogWrapper.v(TAG, "no hold time needed for type=", type);
					break;
			}

			// create the meter config object
			MeterConfig config = new MeterConfig(channel, type);
			// only see the hold time if it isn't zero
			if ((null != holdtime) && (0 != holdtime))
			{
				config.setHoldtime(holdtime);
			}

			// store the meter config
			application.setConfigForChannel(config);

			// set the config
			application.getControl().notifyLevelConfigChange();

			// force a recreation of all level views
			populateLevelViews();

			LogWrapper.v(TAG,
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
					LogWrapper.wtf(TAG, "unknown radio button id=", id);
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
			LogWrapper
			        .v(TAG,
			                "MainActivity::LevelRadioCheckedChangeListener::onCheckedChanged enter",
			                "this=", this, "group=", group, "id=", id);
			// update the selection dialog
			updateSelectionDialog(layout, id);
			LogWrapper
			        .v(TAG,
			                "MainActivity::LevelRadioCheckedChangeListener::onCheckedChanged exit");
		}

	}

	private class HoldTimeSeekbarChangeListener implements
	        OnSeekBarChangeListener
	{
		private final TextView textView;

		public HoldTimeSeekbarChangeListener(TextView textView)
		{
			this.textView = textView;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
		        boolean fromUser)
		{
			LogWrapper
			        .v(TAG,
			                "MainActivity::HoldTimeSeekbarChangeListener::onProgressChanged enter",
			                "this=", this, "seekBar=", seekBar, "progress=",
			                progress, "fromUser=", fromUser);
			// update the label
			if (0 == progress)
			{
				this.textView.setText(MainActivity.this.getResources()
				        .getString(R.string.levelselection_off));
			}
			else
			{
				this.textView.setText(String.valueOf(progress));
			}
			LogWrapper
			        .v(TAG,
			                "MainActivity::HoldTimeSeekbarChangeListener::onProgressChanged exit");
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar)
		{
			// ignore
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar)
		{
			// ignore
		}

	}

	private class CancelConnectionClickListener implements
	        DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			LogWrapper
			        .v(TAG,
			                "MainActivity::CancelConnectionClickListener::onClick enter",
			                "this=", this, "dialog=", dialog, "which=", which);
			// disconnect and close this activity
			application.getControl().getManager().disconnect();
			MainActivity.this.finish();
			LogWrapper
			        .v(TAG,
			                "MainActivity::CancelConnectionClickListener::onClick exit");
		}

	}
}
