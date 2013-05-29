package com.jebussystems.levelingglass.util;

import java.util.EnumMap;
import java.util.Map;

import android.util.Log;

public class StateMachine<S extends Enum<S>, E extends Enum<E>, O, D>
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "statemachine";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final S defaultState;
	private final Map<S, Map<E, Handler<S, O, D>>> handlers;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public StateMachine(S defaultState)
	{
		Log.d(TAG, "StateMachine defaultState=" + defaultState);
		this.handlers = new EnumMap<S, Map<E, Handler<S, O, D>>>(
		        defaultState.getDeclaringClass());
		this.defaultState = defaultState;

	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void addHandler(S state, E event,
	        StateMachine.Handler<S, O, D> handler)
	{
		Log.d(TAG, "addHandler state=" + state + " event=" + event);
		Map<E, Handler<S, O, D>> subHandlers = this.handlers.get(state);
		if (null == subHandlers)
		{
			Log.d(TAG, "adding subhandler for state=" + state);
			subHandlers = new EnumMap<E, Handler<S, O, D>>(
			        event.getDeclaringClass());
			this.handlers.put(state, subHandlers);
		}
		if (true == subHandlers.containsKey(event))
		{
			Log.w(TAG, "event handler already exists for state=" + state
			        + " event=" + event);
		}
		subHandlers.put(event, handler);
	}

	public Instance createInstance(O object)
	{
		return new Instance(object, defaultState);
	}

	public Handler<S, O, D> createDoNothingHandler()
	{
		return new DoNothingHandler<S, O, D>();
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private Handler<S, O, D> findHandler(S state, E event)
	{
		Log.d(TAG, "findHandler state=" + state + " event=" + event);
		Map<E, Handler<S, O, D>> subHandlers = this.handlers.get(state);
		if (null == subHandlers)
		{
			Log.e(TAG, "handler not found for state=" + state + " event="
			        + event);
			throw new IllegalStateException("handler not found for state="
			        + state + " event=" + event);
		}
		Handler<S, O, D> handler = subHandlers.get(event);
		return handler;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	public class Instance
	{
		private final O object;
		private S state;
		private StateChangeListener<S> listener = null;

		private Instance(O object, S initialState)
		{
			Log.d(TAG, "Instance initialState=" + initialState);
			this.object = object;
			this.state = initialState;
		}

		public O getObject()
		{
			return object;
		}

		public S getState()
		{
			return state;
		}
		
		public void addListener(StateChangeListener<S> listener)
		{
			this.listener = listener;
		}

		public void evaluate(E event, D data)
		{
			Log.d(TAG, "evaluate event=" + event + " data=" + data);
			Handler<S, O, D> handler = findHandler(getState(), event);
			S nextState = handler.handleEvent(getObject(), data);
			if ((null != nextState) && (false == nextState.equals(this.state)))
			{
				Log.d(TAG, "current state=" + this.state + " next state=" + nextState);
				// trigger the listener
				if (null != listener)
				{
					listener.notifyStateChange(nextState);
				}
				// update the state
				this.state = nextState;
			}
		}

	}

	public interface Handler<S, O, D>
	{
		S handleEvent(O object, D data);
	}
	
	public interface StateChangeListener<S>
	{
		void notifyStateChange(S state);
	}

	private static class DoNothingHandler<S, O, D> implements Handler<S, O, D>
	{

		@Override
		public S handleEvent(O object, D data)
		{
			Log.d(TAG, "do nothing handler");
			return null;
		}

	}
}
