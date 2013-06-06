package com.jebussystems.levelingglass.activity;

import com.jebussystems.levelingglass.R;

import android.app.Activity;

public class SplashActivity extends Activity
{

	@Override
	protected void onStart()
	{
		super.onStart();
		// setup the layout
		setContentView(R.layout.splash);
	}
}
