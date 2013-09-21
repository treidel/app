package com.jebussystems.levelingglass.bluetooth.spp;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class NotifyDisconnectedMessage extends SPPMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "bluetooth.spp.notifydisconnectedmessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final ObjectPool<NotifyDisconnectedMessage> pool = new StackObjectPool<NotifyDisconnectedMessage>(
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

	private NotifyDisconnectedMessage()
	{
		LogWrapper.v(TAG,
		        "NotifyDisconnectedMessage::NotifyDisconnectedMessage enter",
		        "this=", this);
		LogWrapper.v(TAG,
		        "NotifyDisconnectedMessage::NotifyDisconnectedMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<NotifyDisconnectedMessage> getPoolInstance()
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
	public ObjectPool<NotifyDisconnectedMessage> getPool()
	{
		return pool;
	}

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "NotifyDisconnectedMessage::process enter", "this=",
		        this);
		// fire the state machine event
		getManager().getStateMachineInstance().evaluate(
		        SPPManager.Event.NOTIFY_DISCONNECTED, connection);
		LogWrapper.v(TAG, "NotifyDisconnectedMessage::run exit");
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
	        PoolableMessageFactory<NotifyDisconnectedMessage>
	{

		@Override
		public NotifyDisconnectedMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG,
			        "NotifyDisconnectedMessage::Factory::makeObject enter",
			        "this=", this);
			NotifyDisconnectedMessage message = new NotifyDisconnectedMessage();
			LogWrapper.v(TAG,
			        "NotifyDisconnectedMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}
	}
}
