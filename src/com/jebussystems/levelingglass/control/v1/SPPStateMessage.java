package com.jebussystems.levelingglass.control.v1;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import com.jebussystems.levelingglass.bluetooth.spp.SPPState;
import com.jebussystems.levelingglass.control.v1.ControlV1.Event;
import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessage;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class SPPStateMessage extends PoolableMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "control.v1.sppstatemessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final ObjectPool<SPPStateMessage> pool = new StackObjectPool<SPPStateMessage>(
	        new Factory());

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////
	private SPPState state = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private SPPStateMessage()
	{
		LogWrapper.v(TAG, "SPPStateMessage::SPPStateMessage enter", "this=",
		        this);
		LogWrapper.v(TAG, "SPPStateMessage::SPPStateMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<SPPStateMessage> getPoolInstance()
	{
		return pool;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@Override
	public ObjectPool<SPPStateMessage> getPool()
	{
		return pool;
	}

	public void init(SPPState state)
	{
		this.state = state;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "SPPStateMessage::process enter", "this", this);

		switch (this.state)
		{
			case CONNECTED:
				// send in the event
				ControlV1.getInstance().getStateMachineInstance()
				        .evaluate(Event.CONNECTED, null);
				break;
			case DISCONNECTED:
			case RECONNECTING:
				// send in the event
				ControlV1.getInstance().getStateMachineInstance()
				        .evaluate(Event.DISCONNECTED, null);
				break;
			case CONNECTING:
				// ignore
				break;
			default:
				LogWrapper.wtf(TAG, "state=", this.state);
				return;
		}
		LogWrapper.v(TAG, "SPPStateMessage::process exit");
	}

	@Override
	public void release()
	{
		LogWrapper.v(TAG, "SPPStateMessage::release enter", "this=", this);
		this.state = null;
		LogWrapper.v(TAG, "SPPStateMessage::release exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class Factory extends
	        PoolableMessageFactory<SPPStateMessage>
	{

		@Override
		public SPPStateMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG, "SPPStateMessage::Factory::makeObject enter",
			        "this=", this);
			SPPStateMessage message = new SPPStateMessage();
			LogWrapper.v(TAG, "SPPStateMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}
	}
}
