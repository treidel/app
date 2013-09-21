package com.jebussystems.levelingglass.bluetooth.spp;

import java.nio.ByteBuffer;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessageFactory;

public class SendRequestMessage extends SPPMessage
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "bluetooth.spp.sendrequestmessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////
	private static final ObjectPool<SendRequestMessage> pool = new StackObjectPool<SendRequestMessage>(
	        new Factory());

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private ByteBuffer message = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private SendRequestMessage()
	{
		LogWrapper.v(TAG, "SendRequestMessage::SendRequestMessage enter",
		        "this=", this);
		LogWrapper.v(TAG, "SendRequestMessage::SendRequestMessage exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ObjectPool<SendRequestMessage> getPoolInstance()
	{
		return pool;
	}

	public void init(SPPManager manager, ByteBuffer message)
	{
		super.init(manager);
		this.message = message;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PoolableMessage implementation
	// /////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@Override
	public ObjectPool<SendRequestMessage> getPool()
	{
		return pool;
	}

	@Override
	public void process()
	{
		LogWrapper.v(TAG, "SPPManager::SendRequestMessage::process enter",
		        "this=", this);
		// fire the state machine event
		getManager().getStateMachineInstance().evaluate(
		        SPPManager.Event.SENDREQUEST, message);
		LogWrapper.v(TAG, "SPPManager::SendRequestMessage::run exit");
	}

	@Override
	public void release()
	{
		super.release();
		this.message = null;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class Factory extends
	        PoolableMessageFactory<SendRequestMessage>
	{

		@Override
		public SendRequestMessage makeObject() throws Exception
		{
			LogWrapper.v(TAG, "SendRequestMessage::Factory::makeObject enter",
			        "this=", this);
			SendRequestMessage message = new SendRequestMessage();
			LogWrapper.v(TAG, "SendRequestMessage::Factory::makeObject exit",
			        "message=", message);
			return message;
		}
	}
}
