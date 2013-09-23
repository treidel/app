package com.jebussystems.levelingglass.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.MeterConfig;
import com.jebussystems.levelingglass.control.MeterType;
import com.jebussystems.levelingglass.control.v1.ControlV1;
import com.jebussystems.levelingglass.util.LogWrapper;

import flexjson.JSONDeserializer;

public class LevelSelectionActivity extends Activity
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "activity.levelselection";

	static final String METER_CONFIG_SERIALIZED_JSON = "METER_CONFIG_SERIALIZED_JSON";

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static final JSONDeserializer<MeterConfig> meterConfigDeserializer = new JSONDeserializer<MeterConfig>();

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private MeterConfig config;
	private RadioGroup radioGroupLevel;
	private SeekBar seekBarHoldTime;

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
		LogWrapper.v(TAG, "LevelSelectionActivity::onCreate enter", "this=",
		        this);

		super.onCreate(savedInstanceState);

		// setup the layout
		setContentView(R.layout.levelselection);

		// extract the meter config
		String serialized = getIntent().getStringExtra(
		        METER_CONFIG_SERIALIZED_JSON);
		this.config = meterConfigDeserializer.deserialize(serialized);

		// get references to the views in the layout
		this.radioGroupLevel = (RadioGroup) findViewById(R.id.radiogroup_level);
		this.seekBarHoldTime = (SeekBar) findViewById(R.id.seekbar_holdtime);
		TextView textViewHoldTime = (TextView) findViewById(R.id.textview_holdtime);
		ViewGroup layout = (ViewGroup) findViewById(R.id.layout_levelsection);
		Button buttonOK = (Button) findViewById(R.id.button_ok);
		Button buttonCancel = (Button) findViewById(R.id.button_cancel);

		// populate the views with data from the config
		switch (config.getMeterType())
		{
			case NONE:
				this.radioGroupLevel.check(R.id.radio_levelselection_none);
				break;
			case PPM:
				this.radioGroupLevel.check(R.id.radio_levelsection_ppm);
				if (null != config.getHoldtime())
				{
					this.seekBarHoldTime.setProgress(config.getHoldtime());
				}
				else
				{
					this.seekBarHoldTime.setProgress(0);
				}
				break;
			case DIGITALPEAK:
				this.radioGroupLevel.check(R.id.radio_levelsection_digitalpeak);
				if (null != config.getHoldtime())
				{
					this.seekBarHoldTime.setProgress(config.getHoldtime());
				}
				else
				{
					this.seekBarHoldTime.setProgress(0);
				}
				break;
			case VU:
				this.radioGroupLevel.check(R.id.radio_levelsection_vu);
				break;
			default:
				LogWrapper.wtf(TAG, "unknown level=", config.getMeterType());
				return;
		}

		// update the hold time label
		updateSelectionDialog(layout,
		        this.radioGroupLevel.getCheckedRadioButtonId());

		// add the listeners
		buttonOK.setOnClickListener(new OkButtonListener());
		buttonCancel.setOnClickListener(new CancelButtonListener());
		this.seekBarHoldTime
		        .setOnSeekBarChangeListener(new HoldTimeSeekbarChangeListener(
		                textViewHoldTime));
		this.radioGroupLevel
		        .setOnCheckedChangeListener(new LevelRadioCheckedChangeListener(
		                layout));

		LogWrapper.v(TAG, "LevelSelectionActivity::onCreate exit");
	}

	@Override
	protected void onStart()
	{
		LogWrapper.v(TAG, "LevelSelectionActivity::onStart enter", "this=",
		        this);

		super.onStart();

		LogWrapper.v(TAG, "LevelSelectionActivity::onStart exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

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

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

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
			                "LevelSelectionActivity::HoldTimeSeekbarChangeListener::onProgressChanged enter",
			                "this=", this, "seekBar=", seekBar, "progress=",
			                progress, "fromUser=", fromUser);
			// update the label
			if (0 == progress)
			{
				this.textView.setText(LevelSelectionActivity.this
				        .getResources().getString(R.string.levelselection_off));
			}
			else
			{
				this.textView.setText(String.valueOf(progress));
			}
			LogWrapper
			        .v(TAG,
			                "LevelSelectionActivity::HoldTimeSeekbarChangeListener::onProgressChanged exit");
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

	private class OkButtonListener implements OnClickListener
	{

		@Override
		public void onClick(View view)
		{
			// extract the data from the views
			MeterType meterType = convertRadioIdToMeterType(radioGroupLevel
			        .getCheckedRadioButtonId());
			config.setMeterType(meterType);
			if (0 == seekBarHoldTime.getProgress())
			{
				config.setHoldtime(null);
			}
			else
			{
				config.setHoldtime(seekBarHoldTime.getProgress());
			}
			// update the config
			((LevelingGlassApplication) getApplication())
			        .setConfigForChannel(config);

			// set the config
			ControlV1.getInstance().notifyLevelConfigChange();

			// done
			finish();
		}
	}

	private class CancelButtonListener implements OnClickListener
	{

		@Override
		public void onClick(View view)
		{
			// just close - don't update
			finish();
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
}
