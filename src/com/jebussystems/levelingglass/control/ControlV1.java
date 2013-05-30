package com.jebussystems.levelingglass.control;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import v1.V1;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.bluetooth.spp.SPPMessageHandler;
import com.jebussystems.levelingglass.bluetooth.spp.SPPState;
import com.jebussystems.levelingglass.bluetooth.spp.SPPStateListener;
import com.jebussystems.levelingglass.util.StateMachine;

public class ControlV1 implements SPPMessageHandler, SPPStateListener
{

	// /////////////////////////////////////////////////////////////////////////
	// type definitions
	// /////////////////////////////////////////////////////////////////////////

	public enum Level
	{
		NONE(V1.SetLevelRequest.LevelType.NONE), VU(
		        V1.SetLevelRequest.LevelType.VU), PEAK(
		        V1.SetLevelRequest.LevelType.PEAK);

		private final V1.SetLevelRequest.LevelType levelType;

		private Level(V1.SetLevelRequest.LevelType levelType)
		{
			this.levelType = levelType;
		}

		public V1.SetLevelRequest.LevelType getLevelType()
		{
			return levelType;
		}

	}

	public interface LevelListener
	{
		void handleChannelDiscovery(Collection<Integer> channels);

		void handleLevelData(List<Integer> levels);
	}

	private enum State
	{
		WAIT_FOR_CONNECTION, SYNCHRONIZING, CONNECTED
	}

	private enum Event
	{
		CONNECTED, DISCONNECTED, QUERY_CHANNELS_RESPONSE, LEVEL_CHANGE
	}

	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "control.v1";

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static StateMachine<State, Event, ControlV1, Object> stateMachine = new StateMachine<State, Event, ControlV1, Object>(
	        State.WAIT_FOR_CONNECTION);

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final SPPManager manager;
	private Level level = Level.NONE;
	private final Collection<LevelListener> listeners = new LinkedList<LevelListener>();
	private Queue<V1.Request> pendingRequestQueue = new LinkedList<V1.Request>();
	private StateMachine<State, Event, ControlV1, Object>.Instance stateMachineInstance = stateMachine
	        .createInstance(this);

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	static
	{
		stateMachine.addHandler(State.WAIT_FOR_CONNECTION, Event.CONNECTED,
		        new ConnectHandler());
		stateMachine.addHandler(State.SYNCHRONIZING,
		        Event.QUERY_CHANNELS_RESPONSE,
		        new QueryChannelsResponseHandler());
		stateMachine.addHandler(State.SYNCHRONIZING, Event.DISCONNECTED,
		        new DisconnectHandler());
		stateMachine.addHandler(State.CONNECTED, Event.DISCONNECTED,
		        new DisconnectHandler());
		stateMachine.addHandler(State.CONNECTED, Event.LEVEL_CHANGE,
		        new ChangeLevelInConnectedHandler());
		stateMachine.addHandler(State.SYNCHRONIZING, Event.LEVEL_CHANGE,
		        stateMachine.createDoNothingHandler());
		stateMachine.addHandler(State.WAIT_FOR_CONNECTION, Event.LEVEL_CHANGE,
		        stateMachine.createDoNothingHandler());
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public ControlV1()
	{
		Log.d(TAG, "ControlV1 enter");

		// create the SPP mananger
		this.manager = new SPPManager(this);
		// we care about state transitions too
		this.manager.addSPPStateListener(this);

		// TBD: restore the stored level (if it exists)

		// wait for a connection

		Log.d(TAG, "ControlV1 exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void setLevel(Level level)
	{
		Log.d(TAG, "setLevel enter level=" + level.toString());

		// store the level
		this.level = level;

		// TBD: persist this

		// trigger the state machine
		LevelChangeMessage message = new LevelChangeMessage();
		this.executor.execute(message);

		Log.d(TAG, "setLevel exit");
	}

	public Level getLevel()
	{
		return level;
	}

	public void addListener(LevelListener listener)
	{
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
	}

	public void removeListener(LevelListener listener)
	{
		synchronized (this.listeners)
		{
			this.listeners.remove(listener);
		}
	}

	public SPPManager getManager()
	{
		return manager;
	}

	// ////////////////////////////////////////////////////////////////////////
	// SPPStateListener implementation
	// ////////////////////////////////////////////////////////////////////////

	public void notifySPPStateChanged(SPPState state)
	{
		Log.d(TAG, "notifySPPStateChanged enter state=" + state.toString());

		// send ourselves a message to handle this
		SPPStateMessage message = new SPPStateMessage(state);
		this.executor.execute(message);

		Log.d(TAG, "notifySPPStateChanged exit");

	}

	// ////////////////////////////////////////////////////////////////////////
	// SPPMessageHandler implementation
	// ////////////////////////////////////////////////////////////////////////

	public void handleSPPMessage(ByteBuffer message)
	{
		Log.d(TAG, "handleSPPMessage enter message=" + message.toString());

		try
		{
			// decode the message
			V1.ResponseOrNotification msg = V1.ResponseOrNotification
			        .parseFrom(message.array());
			// figure out what type of message this is
			switch (msg.getType())
			{
				case RESPONSE:
					handleResponse(msg.getResponse());
					break;
				case NOTIFICATION:
					handleNotification(msg.getNotification());
					break;
				default:
					Log.e(TAG, "unknown type: " + msg.getType().toString());
					return;
			}
		}
		catch (InvalidProtocolBufferException e)
		{
			Log.e(TAG, "unable to decode message, reason=" + e.getMessage());
		}
		Log.d(TAG, "handleSPPMessage exit");
	}

	// ////////////////////////////////////////////////////////////////////////
	// private method implementations
	// ////////////////////////////////////////////////////////////////////////

	private void sendLevelRequest(Level level)
	{
		Log.d(TAG, "sendLevelRequest enter level=" + level);

		// build the message to set the level
		V1.SetLevelRequest.Builder setLevelRequestBuilder = V1.SetLevelRequest
		        .newBuilder();
		setLevelRequestBuilder.setLeveltype(level.getLevelType());
		V1.Request.Builder requestBuilder = V1.Request.newBuilder();
		requestBuilder.setSetlevel(setLevelRequestBuilder);
		requestBuilder.setType(V1.RequestType.SETLEVEL);

		// send the message
		sendRequest(requestBuilder.build());

		Log.d(TAG, "sendLevelRequest exit");
	}

	private void sendQueryChannelRequest()
	{
		Log.d(TAG, "sendQueryChannelRequest enter");

		// build the message to set the level
		V1.QueryAudioChannelsRequest.Builder queryChannelRequestBuilder = V1.QueryAudioChannelsRequest
		        .newBuilder();
		V1.Request.Builder requestBuilder = V1.Request.newBuilder();
		requestBuilder.setQueryaudiochannels(queryChannelRequestBuilder);
		requestBuilder.setType(V1.RequestType.QUERYAUDIOCHANNELS);

		// send the message
		sendRequest(requestBuilder.build());

		Log.d(TAG, "sendQueryChannelRequest exit");
	}

	private void sendRequest(V1.Request request)
	{
		Log.d(TAG, "sendRequest request=" + request.toString());
		// add the request to the bottom of the pending list
		this.pendingRequestQueue.add(request);
		// calculate how large the request is in bytes
		short length = (short) request.getSerializedSize();
		// allocate a byte buffer to hold it
		ByteBuffer buffer = ByteBuffer.allocate(length);
		// write in the serialized request
		byte[] data = request.toByteArray();
		buffer.put(data);
		// off she goes
		this.manager.sendRequest(buffer);

		Log.d(TAG, "sendRequest exit");

	}

	private void handleResponse(V1.Response response)
	{
		Log.d(TAG, "handleResponse enter response=" + response.toString());

		// if there are no pending request we have a problem
		if (true == this.pendingRequestQueue.isEmpty())
		{
			Log.e(TAG, "response received when request list is empty");
			this.manager.disconnect();
			return;
		}

		// get the top of the request list
		V1.Request request = this.pendingRequestQueue.remove();

		// request + response should match up
		if (false == request.getType().equals(response.getType()))
		{
			Log.e(TAG, "request + response types don't match");
			this.manager.disconnect();
			return;
		}

		// the server should never say no
		if (true != response.getSuccess())
		{
			Log.w(TAG,
			        "server rejected request, resetting bluetooth connection");
			// force the server to disconnect
			this.manager.disconnect();
		}
		else
		{
			Log.d(TAG, "type=" + response.getType());
			switch (response.getType())
			{
				case QUERYAUDIOCHANNELS:
					// setup a level for each channel
					synchronized (this.listeners)
					{
						for (LevelListener listener : listeners)
						{
							listener.handleChannelDiscovery(response
							        .getQueryaudiochannels().getChannelsList());
						}
					}
					// tell the server to start sending us level notifications
					sendLevelRequest(level);

					// done
					break;

				case SETLEVEL:
					// nothing to do
					break;

				default:
					Log.e(TAG, "unknown type: " + response.getType().toString());
					return;
			}
		}
		Log.d(TAG, "handleResponse exit");
	}

	private void handleNotification(V1.Notification notification)
	{
		Log.d(TAG,
		        "handleNotification enter notification="
		                + notification.toString());
		switch (notification.getType())
		{
			case LEVEL:
				synchronized (this.listeners)
				{
					for (LevelListener listener : listeners)
					{
						listener.handleLevelData(notification.getLevel()
						        .getLevelInDecibelsList());
					}
				}
				break;
			default:
				Log.e(TAG, "unknown type: " + notification.getType().toString());
				return;
		}
		Log.d(TAG, "handleNotification exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class LevelChangeMessage implements Runnable
	{
		@Override
		public void run()
		{
			Log.d(TAG, "run");
			stateMachineInstance.evaluate(Event.LEVEL_CHANGE, null);
		}
	}

	private class SPPStateMessage implements Runnable
	{
		private final SPPState state;

		public SPPStateMessage(SPPState state)
		{
			this.state = state;
		}

		@Override
		public void run()
		{

			Log.d(TAG, "run state=" + this.state);

			switch (this.state)
			{
				case CONNECTED:
					// send in the event
					stateMachineInstance.evaluate(Event.CONNECTED, null);
					break;
				case DISCONNECTED:
					// send in the event
					stateMachineInstance.evaluate(Event.DISCONNECTED, null);
					break;
				case CONNECTING:
					// ignore
					break;
				default:
					Log.wtf(TAG, "state=" + this.state);
					return;
			}
		}
	}

	private static class ConnectHandler implements
	        StateMachine.Handler<State, ControlV1, Object>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			// send the request for the list of channels
			object.sendQueryChannelRequest();

			// now synchronizing
			return State.SYNCHRONIZING;
		}
	}

	private static class QueryChannelsResponseHandler implements
	        StateMachine.Handler<State, ControlV1, Object>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			// send the level
			object.sendLevelRequest(object.getLevel());
			// now connected
			return State.CONNECTED;
		}
	}

	private static class DisconnectHandler implements
	        StateMachine.Handler<State, ControlV1, Object>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			// now connected
			return State.WAIT_FOR_CONNECTION;
		}
	}

	private static class ChangeLevelInConnectedHandler implements
	        StateMachine.Handler<State, ControlV1, Object>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			// send the new level
			object.sendLevelRequest(object.getLevel());
			// no change in state
			return null;
		}
	}

}