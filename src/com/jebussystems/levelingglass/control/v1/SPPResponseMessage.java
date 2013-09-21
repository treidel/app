package com.jebussystems.levelingglass.control.v1;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import v1.V1;

import com.jebussystems.levelingglass.control.v1.ControlV1.Event;
import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessage;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class SPPResponseMessage extends PoolableMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "control.v1.sppresponsemessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final ObjectPool<SPPResponseMessage> pool = new StackObjectPool<SPPResponseMessage>(
	        new Factory());

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////
	private V1.Response response = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private SPPResponseMessage()
	{
		LogWrapper.v(TAG, "SPPStateMessage::SPPStateMessage enter", "this=",
		        this);
		LogWrapper.v(TAG, "SPPStateMessage::SPPStateMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<SPPResponseMessage> getPoolInstance()
	{
		return pool;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@Override
	public ObjectPool<SPPResponseMessage> getPool()
	{
		return pool;
	}

	public void init(V1.Response response)
	{
		this.response = response;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "SPPResponseMessage::process enter", "this", this);

		LogWrapper.d(TAG, "type=" + response.getType());

		switch (response.getType())
		{
			case QUERYAUDIOCHANNELS:
				// trigger the state machine
				ControlV1
				        .getInstance()
				        .getStateMachineInstance()
				        .evaluate(Event.QUERY_CHANNELS_RESPONSE,
				                response.getQueryaudiochannels());
				// done
				break;

			case SETLEVEL:
				// nothing to do
				break;

			default:
				LogWrapper.wtf(TAG, "unknown type: "
				        + response.getType().toString());
				return;
		}
		LogWrapper.v(TAG, "SPPResponseMessage::process exit");
	}

	@Override
	public void release()
	{
		LogWrapper.v(TAG, "SPPResponseMessage::release enter", "this=", this);
		this.response = null;
		LogWrapper.v(TAG, "SPPResponseMessage::release exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class Factory extends
	        PoolableMessageFactory<SPPResponseMessage>
	{

		@Override
		public SPPResponseMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG, "SPPStateMessage::Factory::makeObject enter",
			        "this=", this);
			SPPResponseMessage message = new SPPResponseMessage();
			LogWrapper.v(TAG, "SPPStateMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}
	}
}
