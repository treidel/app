package com.jebussystems.levelingglass.activity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.ControlV1;

public class WaitingForConnectionActivity extends Activity
{
	private static final String TAG = "activity.waitingforconnection";
	public static final String BLUETOOTHDEVICE_NAME = WaitingForConnectionActivity.class
	        .toString() + ".bluetoothdevice";

	private LevelingGlassApplication application;
	private final ControlEventListener listener = new ControlEventListener();
	private TextView statusTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		
		// fetch the application
		this.application = (LevelingGlassApplication) getApplication();

		// setup the layout
		setContentView(R.layout.waitingforconnection);

		// find the status text field
		this.statusTextView = (TextView) findViewById(R.id.status_textview);

		// add a button listener
		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new CancelOnClickListener());

		// fetch the extra data
		BluetoothDevice device = getIntent().getParcelableExtra(
		        BLUETOOTHDEVICE_NAME);

		// populate the peer name
		TextView peerTextView = (TextView) findViewById(R.id.peer_textview);
		peerTextView.setText(device.toString());
		
		// start the connection
		application.getControl().getManager().connect(device);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Log.i(TAG, "onStart");

		// add ourselves as a listener
		application.getControl().addListener(listener);
		
		// update the state 
		this.statusTextView.setText(application.getControl().getState().toString());
	}

	@Override
	protected void onStop()
	{
		super.onStart();
		Log.i(TAG, "onStop");

		// remove ourselves as a listener
		application.getControl().removeListener(listener);
	}

	private class ControlEventListener implements ControlV1.EventListener
	{
		@Override
		public void notifyStateChange(ControlV1.State state)
		{
			final ControlV1.State stateCopy = state;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// update the display
					statusTextView.setText(stateCopy.toString());
					// if we're connected then switch to the main activity
					if (true == ControlV1.State.CONNECTED.equals(stateCopy))
					{
						// start the main activity
						Intent intent = new Intent(
						        WaitingForConnectionActivity.this,
						        MainActivity.class);
						startActivity(intent);
					}
				}
			});

		}

		@Override
		public void notifyLevelsUpdated()
		{
			Log.d(TAG, "ignoring");
		}

	}

	private class CancelOnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			// force a disconnection
			application.getControl().getManager().disconnect();
			
			// go back to the peer selection activity
			finish();
		}

	}

}
