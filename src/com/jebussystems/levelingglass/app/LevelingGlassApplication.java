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
import com.jebussystems.levelingglass.control.MeterConfig;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

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

	private static final JSONSerializer meterConfigSerializer = new JSONSerializer();
	private static final JSONDeserializer<MeterConfig> meterConfigDeserializer = new JSONDeserializer<MeterConfig>();

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private SharedPreferences preferences;
	private ControlV1 control;
	private BluetoothDevice device;
	private Map<Integer, MeterConfig> meterConfigMap = new TreeMap<Integer, MeterConfig>();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate()
	{
		Log.v(TAG, "LevelingGlassApplication::onCreate enter");

		// create the preferences object
		this.preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		// fetch the stored device
		String address = preferences.getString(DEVICE_KEY, null);
		if (null != address)
		{
			Log.d(TAG, "found existing bluetooth device=" + address);
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
			        .getRemoteDevice(address);
			// make sure the device is still paired
			if (BluetoothDevice.BOND_BONDED != device.getBondState())
			{
				Log.d(TAG, "device=" + device
				        + " is no longer paired, ignoring");
			}
			else
			{
				Log.d(TAG, "existing bluetooth device=" + address
				        + " still paired");
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
			String value = tokenizer.nextToken();

			Log.d(TAG, "found channel=" + id + " value=" + value);

			// deserialize the meter config
			MeterConfig config = meterConfigDeserializer.deserialize(value);

			// put the level in the lookup
			this.meterConfigMap.put(Integer.valueOf(id), config);
		}
		// create the control object
		this.control = new ControlV1(this);
		// set channel/level data
		this.control.notifyLevelConfigChange();

		Log.v(TAG, "LevelingGlassApplication::onCreate exit");
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
		Log.v(TAG, "LevelingGlassApplication::setDevice enter device=" + device);

		// store the device
		this.device = device;
		// save in the preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		if (null != device)
		{
			editor.putString(DEVICE_KEY, device.getAddress());
		}
		else
		{
			editor.remove(DEVICE_KEY);
		}
		editor.commit();

		Log.v(TAG, "LevelingGlassApplication::setDevice exit");
	}

	public Set<Integer> getChannelSet()
	{
		return this.meterConfigMap.keySet();
	}

	public MeterConfig getConfigForChannel(int channel)
	{
		return this.meterConfigMap.get(channel);
	}

	public void setConfigForChannel(int channel, MeterConfig config)
	{
		Log.v(TAG,
		        "LevelingGlassApplication::setConfigForChannel enter channel="
		                + channel + " config=" + config);

		// store the config
		this.meterConfigMap.put(channel, config);
		// serialize the config for all levels
		Set<String> channels = new HashSet<String>(this.meterConfigMap.size());
		for (Map.Entry<Integer, MeterConfig> entry : this.meterConfigMap
		        .entrySet())
		{
			String serializedObject = meterConfigSerializer.serialize(entry
			        .getValue());
			String value = entry.getKey() + ":" + serializedObject;
			channels.add(value);
		}
		// save in the preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putStringSet(LEVELS_KEY, channels);
		editor.commit();

		Log.v(TAG, "LevelingGlassApplication::setLevelForChannel exit");
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
