package com.jebussystems.levelingglass.activity;

import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.util.LogWrapper;

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
		LogWrapper.v(TAG, "SplashActivity::onCreate enter", "this=", this);

		super.onCreate(savedInstanceState);

		// setup the layout
		setContentView(R.layout.splash);

		LogWrapper.v(TAG, "SplashActivity::onCreate exit");
	}

	@Override
	protected void onStart()
	{
		LogWrapper.v(TAG, "SplashActivity::onStart enter", "this=", this);

		super.onStart();

		// create the handler for the delayed task
		Handler handler = new Handler();
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				LogWrapper.v(TAG, "SplashActivity::onStart::run enter",
				        "this=", this);

				// don't come back here if back is pressed
				finish();

				// otherwise start the peer selection activity
				Intent intent = new Intent(SplashActivity.this,
				        PeerSelectionActivity.class);
				startActivity(intent);

				LogWrapper.v(TAG, "SplashActivity::onStart::run exit");
			}
		}, TimeUnit.SECONDS.toMillis(SPLASH_DELAY_IN_SECS));

		LogWrapper.v(TAG, "SplashActivity::onStart exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

}
