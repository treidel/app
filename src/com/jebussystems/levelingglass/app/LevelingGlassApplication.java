package com.jebussystems.levelingglass.app;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;

import com.jebussystems.levelingglass.control.MeterConfig;
import com.jebussystems.levelingglass.control.v1.ControlV1;
import com.jebussystems.levelingglass.util.LogWrapper;

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

	private static LevelingGlassApplication instance = null;
	private static final JSONSerializer meterConfigSerializer = new JSONSerializer();
	private static final JSONDeserializer<Collection<MeterConfig>> meterConfigDeserializer = new JSONDeserializer<Collection<MeterConfig>>();

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private SharedPreferences preferences;
	private BluetoothDevice device;
	private Map<Integer, MeterConfig> meterConfigMap = new TreeMap<Integer, MeterConfig>();
	private int splashCount = 0;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static LevelingGlassApplication getInstance()
	{
		return instance;
	}
	
	public int incrementSplashCount()
	{
		splashCount++;
		return splashCount;
	}

	@Override
	public void onCreate()
	{
		LogWrapper.v(TAG, "LevelingGlassApplication::onCreate enter", "this=",
		        this);

		// store a reference to ourselves
		instance = this;

		// create the preferences object
		this.preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		// fetch the stored device
		String address = preferences.getString(DEVICE_KEY, null);
		if (null != address)
		{
			LogWrapper.d(TAG, "found existing bluetooth device=", address);
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
			        .getRemoteDevice(address);
			// make sure the device is still paired
			if (BluetoothDevice.BOND_BONDED != device.getBondState())
			{
				LogWrapper.d(TAG, "device=", device,
				        " is no longer paired, ignoring");
			}
			else
			{
				LogWrapper.d(TAG, "existing bluetooth device=", address,
				        " still paired");
				// keep it
				this.device = device;
			}
		}
		// fetch the stored level settings
		String channels = preferences.getString(LEVELS_KEY, null);
		if (null != channels)
		{
			// deserialize the meter config
			Collection<MeterConfig> configs = (Collection<MeterConfig>) meterConfigDeserializer
			        .deserialize(channels);
			for (MeterConfig config : configs)
			{
				LogWrapper.d(TAG, "found channel=", config.getChannel());

				// put the level in the lookup
				this.meterConfigMap.put(config.getChannel(), config);
			}
		}

		// set channel/level data
		ControlV1.getInstance().notifyLevelConfigChange();

		LogWrapper.v(TAG, "LevelingGlassApplication::onCreate exit");
	}

	public BluetoothDevice getDevice()
	{
		return device;
	}

	public void setDevice(BluetoothDevice device)
	{
		LogWrapper.v(TAG, "LevelingGlassApplication::setDevice enter", "this=",
		        this, "device=", device);

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

		LogWrapper.v(TAG, "LevelingGlassApplication::setDevice exit");
	}

	public Set<Integer> getChannelSet()
	{
		return this.meterConfigMap.keySet();
	}

	public MeterConfig getConfigForChannel(int channel)
	{
		return this.meterConfigMap.get(channel);
	}

	public void setConfigForChannel(MeterConfig config)
	{
		LogWrapper.v(TAG,
		        "LevelingGlassApplication::setConfigForChannel enter", "this=",
		        this, "config=", config);

		// store the config
		this.meterConfigMap.put(config.getChannel(), config);
		// serialize the config for all levels
		Collection<MeterConfig> configs = this.meterConfigMap.values();
		String serializedCollection = meterConfigSerializer.serialize(configs);
		// save in the preferences
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putString(LEVELS_KEY, serializedCollection);
		editor.commit();

		LogWrapper.v(TAG, "LevelingGlassApplication::setLevelForChannel exit");
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
