package com.jebussystems.levelingglass.app;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.util.Log;

import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.Level;

public class LevelingGlassApplication extends Application
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "application";
	private static final String PREFERENCES_NAME = "settings";
	private static final String DEVICE_KEY = "device";
	private static final String LEVELS_KEY = "levels";

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private SharedPreferences preferences;
	private ControlV1 control;
	private BluetoothDevice device;
	private Map<Integer, Level> levels = new TreeMap<Integer, Level>();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate()
	{
		Log.d(TAG, "onCreate");
		// create the preferences object
		this.preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		// fetch the stored device
		String address = preferences.getString(DEVICE_KEY, null);
		if (null != address)
		{
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
			        .getRemoteDevice(address);
			// make sure the device is still paired
			if (BluetoothDevice.BOND_BONDED != device.getBondState())
			{
				Log.d(TAG, "device=" + device
				        + " is not longer paired, ignoring");
			}
			else
			{
				// keep it
				this.device = device;
			}
		}
		// fetch the stored level settings
		Set<String> channels = preferences.getStringSet(LEVELS_KEY,
		        new HashSet<String>());
		for (String channel : channels)
		{
			// separate the channel id from the level
			StringTokenizer tokenizer = new StringTokenizer(channel, ":");
			String id = tokenizer.nextToken();
			String type = tokenizer.nextToken();
			// put the level in the lookup
			this.levels.put(Integer.valueOf(id), Level.valueOf(type));
		}
		// create the control object
		this.control = new ControlV1();
		// set channel/level data
		this.control.updateLevels(this.levels);
	}

	public ControlV1 getControl()
	{
		return control;
	}

	public BluetoothDevice getDevice()
	{
		return device;
	}

	public void setDevice(BluetoothDevice device)
	{
		// store the device
		this.device = device;
		// save in the preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putString(DEVICE_KEY, device.getAddress());
		editor.commit();
	}

	public Level getLevelForChannel(int channel)
	{
		return this.levels.get(channel);
	}

	public void setLevelForChannel(int channel, Level level)
	{
		// store the level
		this.levels.put(channel, level);
		// save all levels
		Set<String> channels = new HashSet<String>(this.levels.size());
		for (Map.Entry<Integer, Level> entry : this.levels.entrySet())
		{
			String value = entry.getKey() + ":" + entry.getValue();
			channels.add(value);
		}
		// save in the preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putStringSet(LEVELS_KEY, channels);
		editor.commit();
	}

	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

}
