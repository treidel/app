package com.jebussystems.levelingglass.activity;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.ViewGroup;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.view.AudioLevelView;

public class MainActivity extends Activity
{

	@Override
	protected void onStart()
	{
		// call the base class
		super.onStart();
		// setup the layout
		setContentView(R.layout.main);
		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();
		// get the control object
		ControlV1 control = application.getControl();
		// get the Bluetooth manager object
		SPPManager manager = control.getManager();
		// add our listener
		control.addListener(new LevelListener());
		// get a list of paired bluetooth devices
		Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter()
		        .getBondedDevices();
		BluetoothDevice device = devices.iterator().next();
		// connect
		manager.connect(device);

	}

	private class LevelListener implements ControlV1.LevelListener
	{
		// get the layout
		private ViewGroup layout = (ViewGroup) findViewById(R.layout.main);

		public void handleChannelDiscovery(Collection<Integer> channels)
		{
			for (int channel : channels)
			{
				// add new views
				AudioLevelView view = new AudioLevelView(getApplicationContext());
				this.layout.addView(view, channel);
			}
		}

		public void handleLevelData(List<Integer> levels)
		{
			// iterate through the levels
			ListIterator<Integer> iterator = levels.listIterator();
			while (true == iterator.hasNext())
			{
				// find the view
				AudioLevelView view = (AudioLevelView)this.layout.findViewById(iterator.nextIndex());
				// set the level
				view.setLevel(iterator.next());
			}
		}

	}

}
