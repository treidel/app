package com.jebussystems.levelingglass.control.v1;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import com.jebussystems.levelingglass.control.v1.ControlV1.Event;
import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessage;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class LevelChangeMessage extends PoolableMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "control.v1.levelchangemessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static final ObjectPool<LevelChangeMessage> pool = new StackObjectPool<LevelChangeMessage>(
	        new Factory());

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private LevelChangeMessage()
	{
		LogWrapper.v(TAG, "LevelChangeMessage::LevelChangeMessage enter",
		        "this=", this);
		LogWrapper.v(TAG, "LevelChangeMessage::LevelChangeMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<LevelChangeMessage> getPoolInstance()
	{
		return pool;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@Override
	public ObjectPool<LevelChangeMessage> getPool()
	{
		return pool;
	}

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "LevelChangeMessage::process enter", "this=", this);
		ControlV1.getInstance().getStateMachineInstance()
		        .evaluate(Event.LEVEL_CHANGE, null);
		LogWrapper.v(TAG, "LevelChangeMessage::process exit");
	}

	@Override
	public void release()
	{
		LogWrapper.v(TAG, "LevelChangeMessage::release enter", "this=", this);
		LogWrapper.v(TAG, "LevelChangeMessage::release exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class Factory extends
	        PoolableMessageFactory<LevelChangeMessage>
	{

		@Override
		public LevelChangeMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG, "LevelChangeMessage::Factory::makeObject enter",
			        "this=", this);
			LevelChangeMessage message = new LevelChangeMessage();
			LogWrapper.v(TAG, "LevelChangeMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}

	}

}
