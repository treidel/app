package com.jebussystems.levelingglass.activity;

import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;

public class SplashActivity extends Activity
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "activity.splash";

	private static final int SPLASH_DELAY_IN_SECS = 2;

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private LevelingGlassApplication application;

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
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate");

		// store the reference to the application
		this.application = (LevelingGlassApplication) getApplication();

		// setup the layout
		setContentView(R.layout.splash);
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		Log.d(TAG, "onStart");

		// create the handler for the delayed task
		Handler handler = new Handler();
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				// don't come back here if back is pressed
				finish();
				// if we have a stored bluetooth device then start the
				// main activity
				BluetoothDevice device = application.getDevice();
				if (null != device)
				{
					Log.d(TAG, "starting wait for connection activity");
					// ready to connect
					Intent intent = new Intent(SplashActivity.this,
					        WaitingForConnectionActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					        | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				else
				{
					Log.d(TAG, "staring peer selection activity");
					// otherwise start the peer selection activity
					Intent intent = new Intent(SplashActivity.this,
					        PeerSelectionActivity.class);
					startActivity(intent);
				}
			}
		}, TimeUnit.SECONDS.toMillis(SPLASH_DELAY_IN_SECS));
	}


	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

}
