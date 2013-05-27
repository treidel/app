package com.jebussystems.levelingglass.app;

import android.app.Application;

import com.jebussystems.levelingglass.control.ControlV1;

public class LevelingGlassApplication extends Application
{

	private ControlV1 control;

	@Override
	public void onCreate()
	{
		// create the control object
		this.control = new ControlV1();

	}

	public ControlV1 getControl()
	{
		return control;
	}
}
