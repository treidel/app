package com.jebussystems.levelingglass.app;

import java.util.Set;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.ControlV1.Level;

public class LevelingGlassApplication extends Application
{

	private ControlV1 control;

	@Override
	public void onCreate()
	{
		// create the control object
		this.control = new ControlV1();		
		// set peak level 
		this.control.setLevel(Level.PEAK);
		// get the Bluetooth manager object
		SPPManager manager = control.getManager();
		// get a list of paired bluetooth devices
		Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter()
		        .getBondedDevices();
		BluetoothDevice device = devices.iterator().next();
		// connect
		manager.connect(device);
	}

	public ControlV1 getControl()
	{
		return control;
	}
}
