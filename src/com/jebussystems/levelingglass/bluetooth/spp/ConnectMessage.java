package com.jebussystems.levelingglass.bluetooth.spp;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import android.bluetooth.BluetoothDevice;

import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class ConnectMessage extends SPPMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "bluetooth.spp.connectmessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final ObjectPool<ConnectMessage> pool = new StackObjectPool<ConnectMessage>(
	        new Factory());

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private BluetoothDevice device = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private ConnectMessage()
	{
		LogWrapper
		        .v(TAG, "ConnectMessage::ConnectMessage enter", "this=", this);
		LogWrapper.v(TAG, "ConnectMessage::ConnectMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<ConnectMessage> getPoolInstance()
	{
		return pool;
	}

	public void init(SPPManager manager, BluetoothDevice device)
	{
		super.init(manager);
		this.device = device;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@Override
	public ObjectPool<ConnectMessage> getPool()
	{
		return pool;
	}

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "ConnectMessage::process enter", "this=", this);
		// fire the state machine event
		getManager().getStateMachineInstance().evaluate(
		        SPPManager.Event.CONNECT, device);
		LogWrapper.v(TAG, "ConnectMessage::run exit");
	}

	@Override
	public void release()
	{
		super.release();
		this.device = null;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class Factory extends PoolableMessageFactory<ConnectMessage>
	{

		@Override
		public ConnectMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG, "ConnectMessage::Factory::makeObject enter",
			        "this=", this);
			ConnectMessage message = new ConnectMessage();
			LogWrapper.v(TAG, "ConnectMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}
	}
}
