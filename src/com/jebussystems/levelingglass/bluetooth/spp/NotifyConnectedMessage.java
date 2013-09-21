package com.jebussystems.levelingglass.bluetooth.spp;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class NotifyConnectedMessage extends SPPMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "bluetooth.spp.notifyconnectedmessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final ObjectPool<NotifyConnectedMessage> pool = new StackObjectPool<NotifyConnectedMessage>(
	        new Factory());

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private SPPConnection connection = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private NotifyConnectedMessage()
	{
		LogWrapper.v(TAG,
		        "NotifyConnectedMessage::NotifyConnectedMessage enter",
		        "this=", this);
		LogWrapper
		        .v(TAG, "NotifyConnectedMessage::NotifyConnectedMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<NotifyConnectedMessage> getPoolInstance()
	{
		return pool;
	}

	public void init(SPPManager manager, SPPConnection connection)
	{
		super.init(manager);
		this.connection = connection;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@Override
	public ObjectPool<NotifyConnectedMessage> getPool()
	{
		return pool;
	}

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "NotifyConnectedMessage::process enter", "this=",
		        this);
		// fire the state machine event
		getManager().getStateMachineInstance().evaluate(
		        SPPManager.Event.NOTIFY_CONNECTED, connection);
		LogWrapper.v(TAG, "NotifyConnectedMessage::run exit");
	}

	@Override
	public void release()
	{
		super.release();
		this.connection = null;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class Factory extends
	        PoolableMessageFactory<NotifyConnectedMessage>
	{

		@Override
		public NotifyConnectedMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG,
			        "NotifyConnectedMessage::Factory::makeObject enter",
			        "this=", this);
			NotifyConnectedMessage message = new NotifyConnectedMessage();
			LogWrapper.v(TAG,
			        "NotifyConnectedMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}
	}
}
