package com.jebussystems.levelingglass.activity;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.control.ControlV1;
import com.jebussystems.levelingglass.control.ControlV1.Level;
import com.jebussystems.levelingglass.view.AudioLevelView;

public class MainActivity extends Activity
{
	private static final String TAG = "main";

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
		control.setLevel(Level.PEAK);
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

	private class LevelListener implements ControlV1.LevelListener,
	        Handler.Callback
	{
		private static final int CHANNEL = 1;
		private static final int LEVEL_DATA = 2;

		private final Handler handler;
		// get the layout
		private final ViewGroup layout = (ViewGroup) findViewById(R.id.main_layout);

		public LevelListener()
		{
			// create a handler that allows us to send messages to the UI thread
			this.handler = new Handler(this);
		}

		public void handleChannelDiscovery(Collection<Integer> channels)
		{
			for (int channel : channels)
			{
				// send a message to the UI thread to handle this
				Message message = this.handler.obtainMessage(CHANNEL, channel,
				        0);
				message.sendToTarget();
			}
		}

		public void handleLevelData(List<Integer> levels)
		{
			// iterate through the levels
			ListIterator<Integer> iterator = levels.listIterator();
			while (true == iterator.hasNext())
			{
				// send a message to the UI thread to handle this
				Message message = this.handler.obtainMessage(CHANNEL, iterator.nextIndex(), iterator.next());

				message.sendToTarget();				
			}
		}

		@Override
		public boolean handleMessage(Message msg)
		{
			// see what message we got
			switch (msg.what)
			{
				case CHANNEL:
				{
					// explode the view layout
					LayoutInflater vi = (LayoutInflater) getApplicationContext()
					        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View view = vi.inflate(R.layout.level, null);
					// insert into main view
					layout.addView(view, msg.arg1 - 1, new ViewGroup.LayoutParams(
					        ViewGroup.LayoutParams.MATCH_PARENT,
					        ViewGroup.LayoutParams.MATCH_PARENT));
				}
					break;

				case LEVEL_DATA:
				{
					// find the layout view
					View view = layout.getChildAt(msg.arg1 - 1);
					// find the audio level view
					AudioLevelView audiolevel = (AudioLevelView)view.findViewById(R.id.audio_view);
					// set the level
					audiolevel.setLevel(msg.arg2);
				}
					break;
				default:
					Log.e(TAG, "invalid arg1=" + msg.arg1);
					break;

			}
			return true;
		}
	}

}
