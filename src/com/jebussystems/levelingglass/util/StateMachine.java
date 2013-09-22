package com.jebussystems.levelingglass.util;

import java.util.EnumMap;
import java.util.Map;

public class StateMachine<S extends Enum<S>, E extends Enum<E>, O>
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

	private final Class<E> eventClass;
	private final S defaultState;
	private final Map<S, Map<E, Handler<S, O>>> handlers;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public StateMachine(Class<E> eventClass, S defaultState)
	{
		LogWrapper.v(TAG, "StateMachine::StateMachine enter", "this=", this,
		        "S=", defaultState.getDeclaringClass(), "E=",
		        eventClass.getClass(), "defaultState=", defaultState);

		// initialize internal variables
		this.handlers = new EnumMap<S, Map<E, Handler<S, O>>>(
		        defaultState.getDeclaringClass());
		this.defaultState = defaultState;
		this.eventClass = eventClass;

		LogWrapper.v(TAG, "StateMachine::StateMachine exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void addHandler(S state, E event, StateMachine.Handler<S, O> handler)
	{
		LogWrapper.v(TAG, "StateMachine::addHandler enter", "this=", this,
		        "S=", getStateClass(), "E=" + getEventClass(), "state=", state,
		        "event=", event, "handler=", handler);

		Map<E, Handler<S, O>> subHandlers = this.handlers.get(state);
		if (null == subHandlers)
		{
			LogWrapper.d(TAG, "adding subhandler for state=", state);
			subHandlers = new EnumMap<E, Handler<S, O>>(
			        event.getDeclaringClass());
			this.handlers.put(state, subHandlers);
		}
		if (true == subHandlers.containsKey(event))
		{
			LogWrapper.wtf(TAG, "event handler already exists for state=",
			        state, "event=", event);
		}
		subHandlers.put(event, handler);

		LogWrapper.v(TAG, "StateMachine::addHandler exit");
	}

	public Instance createInstance(O object)
	{
		LogWrapper
		        .v(TAG, "StateMachine::createInstance enter", "this=", this,
		                "S=", getStateClass(), "E=", getEventClass(),
		                "object=", object);

		Instance instance = new Instance(object, defaultState);

		LogWrapper.v(TAG, "StateMachine::createInstance exit", "instance=",
		        instance);
		return instance;
	}

	public Handler<S, O> createDoNothingHandler()
	{
		LogWrapper.v(TAG, "StateMachine::createDoNothingHandler enter",
		        "this=", this, "S=", getStateClass(), "E=", getEventClass());
		Handler<S, O> handler = new DoNothingHandler<S, O>();

		LogWrapper.v(TAG, "StateMachine::createDoNothingHandler exit",
		        "handler=", handler);
		return handler;
	}

	public Class<E> getEventClass()
	{
		return eventClass;
	}

	public Class<S> getStateClass()
	{
		return defaultState.getDeclaringClass();
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private Handler<S, O> findHandler(S state, E event)
	{
		LogWrapper.v(TAG, "StateMachine::findHandler enter", "this=", this,
		        "S=", getStateClass(), "E=", getEventClass(), "state=", state,
		        "event=", event);

		Map<E, Handler<S, O>> subHandlers = this.handlers.get(state);
		if (null == subHandlers)
		{
			LogWrapper.wtf(TAG, "handler not found for state=", state,
			        "class=", state.getClass(), "event=", event, "class=",
			        event.getClass());
			throw new IllegalStateException("handler not found for S="
			        + getStateClass() + " E=" + getEventClass() + " state="
			        + state + " event=" + event);
		}
		Handler<S, O> handler = subHandlers.get(event);
		if (null == handler)
		{
			LogWrapper.wtf(TAG, "handler not found for state=", state,
			        "class=", state.getClass(), "event=", event, "class=",
			        event.getClass());
			throw new IllegalStateException("handler not found for S="
			        + getStateClass() + " E=" + getEventClass() + " state="
			        + state + " event=" + event);
		}

		LogWrapper
		        .v(TAG, "StateMachine::findHandler exit", "handler=", handler);
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
			LogWrapper.v(TAG, "StateMachine::Instance::Instance enter",
			        "this=", this, "S=", getStateClass(), "E="
			                + getEventClass(), "initialState=" + initialState);
			this.object = object;
			this.state = initialState;
			LogWrapper.v(TAG, "StateMachine::Instance::Instance exit");
		}

		public O getObject()
		{
			return object;
		}

		public S getState()
		{
			return state;
		}

		public void setListener(StateChangeListener<S> listener)
		{
			LogWrapper.v(TAG, "StateMachine::Instance::addListener enter",
			        "this=", this, "S=", getStateClass(), "E="
			                + getEventClass());
			this.listener = listener;
			LogWrapper.v(TAG, "StateMachine::Instance::addListener exit");
		}

		public <D> void evaluate(E event, Object data)
		{
			LogWrapper.v(TAG, "StateMachine::Instance::evaluate enter",
			        "this=", this, "S=", getStateClass(), "E=",
			        getEventClass(), "event=", event, "argument=", data);
			// find the handler - this will throw an angry exception if no
			// handler is found
			Handler<S, O> handler = findHandler(getState(), event);
			S nextState = handler.handleEvent(getObject(), data);
			if ((null != nextState) && (false == nextState.equals(this.state)))
			{
				LogWrapper.d(TAG, "state transition => current state=",
				        this.state, "next state=", nextState);

				// trigger the listener
				if (null != listener)
				{
					listener.notifyStateChange(nextState);
				}
				// update the state
				this.state = nextState;
			}

			LogWrapper.v(TAG, "StateMachine::Instance::evaluate exit");
		}

	}

	public interface Handler<S, O>
	{
		S handleEvent(O object, Object data);
	}

	public interface StateChangeListener<S>
	{
		void notifyStateChange(S state);
	}

	private static class DoNothingHandler<S, O> implements Handler<S, O>
	{
		@Override
		public S handleEvent(O object, Object data)
		{
			LogWrapper.v(TAG,
			        "StateMachine::DoNothingHandler::DoNothingHandler enter",
			        "this=", this, "object=", object, "data=", data);
			LogWrapper.d(TAG, "do nothing handler");
			LogWrapper.v(TAG,
			        "StateMachine::DoNothingHandler::DoNothingHandler exit");
			return null;
		}
	}
}
