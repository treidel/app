package com.jebussystems.levelingglass.app;

import java.io.IOException;
import java.util.UUID;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.util.Log;

import com.jebussystems.levelingglass.control.ControlV1;

public class LevelingGlassApplication extends Application
{

	private ControlV1 control;

	@Override
	public void onCreate()
	{
		// create the control object
		this.control = new ControlV1();

		// / HACK HACK
		try
		{
			final BluetoothServerSocket socket = BluetoothAdapter
			        .getDefaultAdapter().listenUsingRfcommWithServiceRecord(
			                "test", UUID.randomUUID());
			Thread thread = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						socket.accept();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			thread.start();
		}
		catch (IOException e)
		{
			Log.e("test", e.getMessage());
		}
	}

	public ControlV1 getControl()
	{
		return control;
	}
}
