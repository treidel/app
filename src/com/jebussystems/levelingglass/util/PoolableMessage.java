package com.jebussystems.levelingglass.util;

import org.apache.commons.pool.ObjectPool;

import android.util.Log;

public abstract class PoolableMessage implements Runnable
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "util.poolablemessage";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void release()
	{
		LogWrapper.v(TAG, "PoolableMessage::release enter", "this=", this);
		LogWrapper.v(TAG, "PoolableMessage::release exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// Runnable implementation
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void run()
	{
		LogWrapper.v(TAG, "PoolableMessage::run enter", "this=", this);

		// call the process method
		process();

		// get the pool
		ObjectPool<PoolableMessage> pool = getPool();

		// return the object
		try
		{
			pool.returnObject(this);
		}
		catch (Exception e)
		{
			Log.wtf(TAG, e.getMessage());
		}

		LogWrapper.v(TAG, "PoolableMessage::run exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	protected abstract void process();

	protected abstract <T extends PoolableMessage> ObjectPool<T> getPool();

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

}