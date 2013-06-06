package com.jebussystems.levelingglass.activity;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.control.ControlV1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class WaitingForConnectionActivity extends Activity
{
	private static final String TAG = "activity.waitingforconnection";
	
	private final ControlEventListener listener = new ControlEventListener();

	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
	    super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		// setup the layout
		setContentView(R.layout.waitingforconnection);
    }
	
	@Override
	protected void onStart()
	{
		super.onStart();
		Log.i(TAG, "onStart");

		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();

		// get the control object
		ControlV1 control = application.getControl();
		// get the control object and add ourselves as a listener
		control.addListener(listener);
	}
	
	@Override
	protected void onStop()
	{
		super.onStart();
		Log.i(TAG, "onStop");

		// get the application object
		LevelingGlassApplication application = (LevelingGlassApplication) getApplication();

		// get the control object
		ControlV1 control = application.getControl();
		// get the control object and add ourselves as a listener
		control.addListener(listener);
	}

	private class ControlEventListener implements ControlV1.EventListener
	{
		@Override
		public void notifyConnected()
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// kill ourselves
					finish();
				}
			});

		}

		@Override
		public void notifyLevelsUpdated()
		{
			Log.d(TAG, "ignoring");
		}

	}

}
