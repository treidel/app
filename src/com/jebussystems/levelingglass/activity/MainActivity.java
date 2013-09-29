package com.jebussystems.levelingglass.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.config.MeterConfig;
import com.jebussystems.levelingglass.control.config.TrimConfig;
import com.jebussystems.levelingglass.control.records.LevelDataRecord;
import com.jebussystems.levelingglass.control.records.PeakDataRecord;
import com.jebussystems.levelingglass.control.v1.ControlV1;
import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.view.AudioLevelView;

import flexjson.JSONSerializer;

public class MainActivity extends Activity
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "activity.main";

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final JSONSerializer meterConfigSerializer = new JSONSerializer();

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private LevelingGlassApplication application;
	private ControlEventListener listener;
	private ListView listview = null;

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

		// attempt start a connection to the device
		BluetoothDevice device = application.getDevice();
		if (false == ControlV1.getInstance().getManager().connect(device))
		{
			// the device is no longer valid so abort this activity and go back
			// to the device
			// selection activity
			finish();
		}
		else
		{
			// setup the layout
			setContentView(R.layout.main);
			// find the list
			this.listview = (ListView) findViewById(R.id.listview_main);
			// add an item click listener
			this.listview
			        .setOnItemClickListener(new MainListViewItemClickListener());
			// add a key press listener
			this.listview.setOnKeyListener(new KeyPressListener());
		}
		LogWrapper.v(TAG, "MainActivity::onCreate exit");

	}

	@Override
	protected void onStart()
	{
		LogWrapper.v(TAG, "MainActivity::onStart enter", "this=", this);

		// call the base class
		super.onStart();

		// get the control object and add ourselves as a listener
		ControlV1 control = ControlV1.getInstance();
		control.addListener(listener);

		// refresh the dialog state
		listener.refresh();

		// do the layout
		populateLevelViews();

		LogWrapper.v(TAG, "MainActivity::onStart exit");
	}

	@Override
	protected void onStop()
	{
		LogWrapper.v(TAG, "MainActivity::onStop enter", "this=", this);

		super.onStop();

		// get the control object and add ourselves as a listener
		ControlV1.getInstance().removeListener(listener);

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
			ControlV1.getInstance().getManager().disconnect();
		}

		LogWrapper.v(TAG, "MainActivity::onDestroy exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private void populateLevelViews()
	{
		// create a new adapter
		CustomAdapter adapter = new CustomAdapter(application.getChannelSet()
		        .size());
		for (int channel : application.getChannelSet())
		{
			// get the channel config
			MeterConfig config = application.getConfigForChannel(channel);
			// tell the adapter about it
			adapter.addChannel(config);
		}
		// attach the adapter to the list to display our levels
		this.listview.setAdapter(adapter);
		// force the list to the last (phony) entry
		this.listview.setSelection(adapter.getCount() - 1);
	}

	private void updateLevelData()
	{
		// get the control object
		ControlV1 control = ControlV1.getInstance();
		// query the level data from the control object
		Map<Integer, LevelDataRecord> records = control.getLevelDataRecord();
		if (null != records)
		{
			for (LevelDataRecord record : records.values())
			{
				synchronized (record)
				{
					// find the layout view
					View view = listview.getChildAt(record.getChannel() - 1);
					Assert.assertNotNull(view);
					// find the audio level view
					AudioLevelView audiolevel = (AudioLevelView) view
					        .findViewById(R.id.audiolevelview);
					// retrieve the current level
					float level = record.getLevel();
					// find the config
					MeterConfig config = application.getConfigForChannel(record
					        .getChannel());

					// see if this record provides a hold time
					Float hold = null;
					if (true == record instanceof PeakDataRecord)
					{
						hold = ((PeakDataRecord) record).getHold();
					}

					// if this meter type supports trim then adjust
					if (true == config instanceof TrimConfig)
					{
						level += ((TrimConfig) config).getTrim();
						if (null != hold)
						{
							hold += ((TrimConfig) config).getTrim();
						}
					}

					// populate the audio level view
					audiolevel.setLevel(level);
					audiolevel.setHold(hold);
				}
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private final class ControlEventListener implements ControlV1.EventListener
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
			        .findViewById(R.id.textview_status);
			// update the peer name
			TextView peerView = (TextView) layout
			        .findViewById(R.id.textview_peer);
			peerView.setText(application.getDevice().toString());
			// set the layout for the dialog
			builder.setView(layout);
			builder.setPositiveButton(android.R.string.cancel,
			        new CancelConnectionClickListener());
			builder.setCancelable(true);
			// create + run the dialog
			this.dialog = builder.create();
			this.dialog.setOnKeyListener(new Dialog.OnKeyListener()
			{

				@Override
				public boolean onKey(DialogInterface dialog, int keyCode,
				        KeyEvent event)
				{
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						finish();
					}
					return true;
				}
			});
		}

		public void refresh()
		{
			final ControlV1.State state = ControlV1.getInstance().getState();
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

	private class MainListViewItemClickListener implements OnItemClickListener
	{

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
		        long id)
		{
			LogWrapper
			        .v(TAG,
			                "MainActivity::MainListViewItemClickListener::onItemClick enter",
			                "this=", this, "parent=", parent, "view=", view,
			                "position=", position, "id=", id);

			// ignore the click if this is the 'fake' entry
			if (position <= application.getChannelSet().size())
			{

				// fetch the config
				MeterConfig config = application
				        .getConfigForChannel(position + 1);

				// start the level selection activity
				Intent intent = new Intent(MainActivity.this,
				        LevelSelectionActivity.class);
				String serialized = meterConfigSerializer.serialize(config);
				intent.putExtra(
				        LevelSelectionActivity.METER_CONFIG_SERIALIZED_JSON,
				        serialized);
				startActivity(intent);
			}

			LogWrapper.v(TAG,
			        "MainActivity::LevelViewClickListener::onClick::run exit");
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
			ControlV1.getInstance().getManager().disconnect();
			MainActivity.this.finish();
			LogWrapper
			        .v(TAG,
			                "MainActivity::CancelConnectionClickListener::onClick exit");
		}

	}

	private class CustomAdapter extends BaseAdapter
	{
		private final List<MeterConfig> meterConfigList;
		private final LayoutInflater vi = (LayoutInflater) getApplicationContext()
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public CustomAdapter(int count)
		{
			this.meterConfigList = new ArrayList<MeterConfig>(count);
		}

		public void addChannel(MeterConfig meterConfig)
		{
			this.meterConfigList.add(meterConfig);
		}

		@Override
		public int getCount()
		{
			// we add +1 to account for the empty last entry
			return this.meterConfigList.size() + 1;
		}

		@Override
		public Object getItem(int position)
		{
			return position;
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			LogWrapper.v(TAG, "MainActivity::CustomAdapter::getView enter",
			        "this=", this, "position=", position, "convertView=",
			        convertView, "parent=", parent);
			// handle the request to create the special empty view
			View view;
			if (position == this.meterConfigList.size())
			{
				view = new LinearLayout(getApplicationContext());
			}
			else
			{
				// get the channel config
				MeterConfig config = application
				        .getConfigForChannel(position + 1);
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
						return null;
				}
				// inflate the layout for the audio view
				view = (ViewGroup) vi.inflate(layoutId, null);
			}
			// done
			LogWrapper.v(TAG, "MainActivity::CustomAdapter::getView exit",
			        "view=", view);
			return view;
		}
	}

	private class KeyPressListener implements OnKeyListener
	{

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			LogWrapper.v(TAG, "MainActivity::KeyPressListener::onKey enter",
			        "this=", this, "v=", v, "keyCode=", keyCode, "event=",
			        event);
			// assume we won't consume this event
			boolean result = false;

			// get the adapter
			CustomAdapter adapter = (CustomAdapter) listview.getAdapter();

			// we only care about the 'up' part of the button press
			if (KeyEvent.ACTION_UP == event.getAction())
			{
				switch (keyCode)
				{
					case KeyEvent.KEYCODE_DPAD_RIGHT:
						// ignore this if we haven't selected an actual meter
						if ((listview.getSelectedItemPosition() + 1) < adapter
						        .getCount())
						{
							// get the config for the level
							MeterConfig config = application
							        .getConfigForChannel(listview
							                .getSelectedItemPosition() + 1);
							// see if this meter supports trim
							if (true == config instanceof TrimConfig)
							{
								// increase the trim
								((TrimConfig) config).addTrimIncrement();
							}
							// consume this event
							result = true;
						}
						break;

					case KeyEvent.KEYCODE_DPAD_LEFT:
						// ignore this if we haven't selected an actual meter
						if ((listview.getSelectedItemPosition() + 1) < adapter
						        .getCount())
						{
							// get the config for the level
							MeterConfig config = application
							        .getConfigForChannel(listview
							                .getSelectedItemPosition() + 1);
							// see if this meter supports trim
							if (true == config instanceof TrimConfig)
							{
								// decrase the trim
								((TrimConfig) config).subtractTrimIncrement();
							}
							// consume this event
							result = true;
						}

					default:
						// ignore
						LogWrapper.d(TAG, "ignoring key press=", event);
						break;
				}
			}
			// done
			LogWrapper.v(TAG, "MainActivity::KeyPressListener::onKey exit",
			        "result=", result);
			return result;
		}

	}
}
