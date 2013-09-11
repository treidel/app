package com.jebussystems.levelingglass.control;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import v1.V1;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.bluetooth.spp.SPPMessageHandler;
import com.jebussystems.levelingglass.bluetooth.spp.SPPState;
import com.jebussystems.levelingglass.bluetooth.spp.SPPStateListener;
import com.jebussystems.levelingglass.util.EnumMapper;
import com.jebussystems.levelingglass.util.StateMachine;

public class ControlV1 implements SPPMessageHandler, SPPStateListener
{

	// /////////////////////////////////////////////////////////////////////////
	// type definitions
	// /////////////////////////////////////////////////////////////////////////

	public interface EventListener
	{
		void notifyLevelsUpdated();

		void notifyStateChange(State state);
	}

	public enum State
	{
		CONNECTING, SYNCHRONIZING, CONNECTED
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

	private static final EnumMapper<MeterType, V1.LevelType> levelMapper = new EnumMapper<MeterType, V1.LevelType>(
	        MeterType.class, V1.LevelType.class);
	private static final StateMachine<State, Event, ControlV1> stateMachine = new StateMachine<State, Event, ControlV1>(
	        Event.class, State.CONNECTING);

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final SPPManager manager;
	private final Collection<EventListener> listeners = new LinkedList<EventListener>();
	private Queue<V1.Request> pendingRequestQueue = new LinkedList<V1.Request>();
	private StateMachine<State, Event, ControlV1>.Instance stateMachineInstance = stateMachine
	        .createInstance(this);
	private Map<Integer, MeterConfig> channelToMeterConfigMapping = null;
	private Map<Integer, LevelDataRecord> levelDataRecords = new TreeMap<Integer, LevelDataRecord>();;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	static
	{
		// set the enum mapper
		levelMapper.addMapping(MeterType.NONE, V1.LevelType.NONE);
		levelMapper.addMapping(MeterType.DIGITALPEAK, V1.LevelType.DIGITALPEAK);
		levelMapper.addMapping(MeterType.PPM, V1.LevelType.PPM);
		levelMapper.addMapping(MeterType.VU, V1.LevelType.VU);

		// setup the state machine
		stateMachine.addHandler(State.CONNECTING, Event.CONNECTED,
		        new ConnectHandler());
		stateMachine.addHandler(State.CONNECTING, Event.DISCONNECTED,
		        new DisconnectHandler());
		stateMachine.addHandler(State.CONNECTING, Event.LEVEL_CHANGE,
		        stateMachine.createDoNothingHandler());
		stateMachine.addHandler(State.SYNCHRONIZING,
		        Event.QUERY_CHANNELS_RESPONSE,
		        new QueryChannelsResponseHandler());
		stateMachine.addHandler(State.SYNCHRONIZING, Event.DISCONNECTED,
		        new DisconnectHandler());
		stateMachine.addHandler(State.SYNCHRONIZING, Event.LEVEL_CHANGE,
		        stateMachine.createDoNothingHandler());
		stateMachine.addHandler(State.CONNECTED, Event.DISCONNECTED,
		        new DisconnectHandler());
		stateMachine.addHandler(State.CONNECTED, Event.LEVEL_CHANGE,
		        new ChangeLevelInConnectedHandler());

	}

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public ControlV1()
	{
		Log.v(TAG, "ControlV1::ControlV1 enter");

		// create the SPP mananger
		this.manager = new SPPManager(this);
		// we care about state transitions
		this.manager.addSPPStateListener(this);

		// setup the state machine state change listener
		this.stateMachineInstance.setListener(new StateMachineListener());

		Log.v(TAG, "ControlV1::ControlV1 exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void updateLevel(int channel, MeterConfig config)
	{
		Log.v(TAG, "ControlV1::updateLevel enter index=" + channel + " type="
		        + config.getMeterType());

		// make sure this is a valid channel
		if (false == this.channelToMeterConfigMapping.containsKey(channel))
		{
			Log.wtf(TAG, "invalid channel=" + channel);
			return;
		}

		// store the new setting
		this.channelToMeterConfigMapping.put(channel, config);

		// clear the levels
		this.levelDataRecords.remove(channel);

		// trigger the state machine
		LevelChangeMessage message = new LevelChangeMessage();
		this.executor.execute(message);

		Log.v(TAG, "ControlV1::updateLevel exit");
	}

	public void updateLevels(Map<Integer, MeterConfig> levels)
	{
		Log.v(TAG, "ControlV1::updateLevels enter levels=" + levels);

		// store the new settings
		this.channelToMeterConfigMapping = levels;

		// clear all records
		this.levelDataRecords.clear();

		// trigger the state machine
		LevelChangeMessage message = new LevelChangeMessage();
		this.executor.execute(message);

		Log.v(TAG, "ControlV1::updateLevels exit");
	}

	public void addListener(EventListener listener)
	{
		Log.v(TAG, "ControlV1::addListener enter listener=" + listener);
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
		Log.v(TAG, "ControlV1::addListener exit");
	}

	public void removeListener(EventListener listener)
	{
		Log.v(TAG, "ControlV1::removeListener enter listener=" + listener);
		synchronized (this.listeners)
		{
			this.listeners.remove(listener);
		}
		Log.v(TAG, "ControlV1::removeListener exit");
	}

	public SPPManager getManager()
	{
		return manager;
	}

	public Map<Integer, MeterConfig> getLevels()
	{
		return channelToMeterConfigMapping;
	}

	public Map<Integer, LevelDataRecord> getLevelDataRecord()
	{
		return levelDataRecords;
	}

	public State getState()
	{
		return this.stateMachineInstance.getState();
	}

	// ////////////////////////////////////////////////////////////////////////
	// SPPStateListener implementation
	// ////////////////////////////////////////////////////////////////////////

	public void notifySPPStateChanged(SPPState state)
	{
		Log.v(TAG, "ControlV1::notifySPPStateChanged enter state=" + state);

		// send ourselves a message to handle this
		SPPStateMessage message = new SPPStateMessage(state);
		this.executor.execute(message);

		Log.v(TAG, "ControlV1::notifySPPStateChanged exit");

	}

	// ////////////////////////////////////////////////////////////////////////
	// SPPMessageHandler implementation
	// ////////////////////////////////////////////////////////////////////////

	public void handleSPPMessage(ByteBuffer message)
	{
		Log.v(TAG, "ControlV1::handleSPPMessage enter message=" + message);

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
		Log.v(TAG, "ControlV1::handleSPPMessage exit");
	}

	// ////////////////////////////////////////////////////////////////////////
	// private method implementations
	// ////////////////////////////////////////////////////////////////////////

	private void sendLevelRequest(int channel, MeterConfig config)
	{
		Log.v(TAG, "ControlV1::sendLevelRequest enter channel=" + channel
		        + " config=" + config);

		// build the message to set the level
		V1.SetLevelRequest.Builder setLevelRequestBuilder = V1.SetLevelRequest
		        .newBuilder();
		setLevelRequestBuilder.setType(levelMapper.mapToExternal(config
		        .getMeterType()));
		setLevelRequestBuilder.setChannel(channel);
		if (null != config.getHoldtime())
		{
			setLevelRequestBuilder.setHoldtime(config.getHoldtime());
		}
		V1.Request.Builder requestBuilder = V1.Request.newBuilder();
		requestBuilder.setSetlevel(setLevelRequestBuilder);
		requestBuilder.setType(V1.RequestType.SETLEVEL);

		// send the message
		sendRequest(requestBuilder.build());

		Log.v(TAG, "ControlV1::sendLevelRequest exit");
	}

	private void sendQueryChannelRequest()
	{
		Log.v(TAG, "ControlV1::sendQueryChannelRequest enter");

		// build the message to set the level
		V1.QueryAudioChannelsRequest.Builder queryChannelRequestBuilder = V1.QueryAudioChannelsRequest
		        .newBuilder();
		V1.Request.Builder requestBuilder = V1.Request.newBuilder();
		requestBuilder.setQueryaudiochannels(queryChannelRequestBuilder);
		requestBuilder.setType(V1.RequestType.QUERYAUDIOCHANNELS);

		// send the message
		sendRequest(requestBuilder.build());

		Log.v(TAG, "ControlV1::sendQueryChannelRequest exit");
	}

	private void sendRequest(V1.Request request)
	{
		Log.v(TAG, "ControlV1::sendRequest request=" + request.toString());
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

		Log.v(TAG, "ControlV1::sendRequest exit");

	}

	private void handleResponse(V1.Response response)
	{
		Log.v(TAG,
		        "ControlV1::handleResponse enter response="
		                + response.toString());

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
					// trigger the state machine
					this.stateMachineInstance.evaluate(
					        Event.QUERY_CHANNELS_RESPONSE,
					        response.getQueryaudiochannels());
					// done
					break;

				case SETLEVEL:
					// nothing to do
					break;

				default:
					Log.wtf(TAG, "unknown type: "
					        + response.getType().toString());
					return;
			}
		}
		Log.v(TAG, "ControlV1::handleResponse exit");
	}

	private void handleNotification(V1.Notification notification)
	{
		Log.v(TAG, "ControlV1::handleNotification enter notification="
		        + notification.toString());
		switch (notification.getType())
		{
			case LEVEL:
				// store the level data internally
				Map<Integer, LevelDataRecord> levelDataRecords = new TreeMap<Integer, LevelDataRecord>();
				for (v1.V1.LevelRecord record : notification.getLevel()
				        .getRecordsList())

				{
					// convert the type into our internal version
					MeterType recordLevel = levelMapper.mapToInternal(record
					        .getType());
					// get the configured level
					MeterType configuredLevel = this.channelToMeterConfigMapping
					        .get(record.getChannel()).getMeterType();
					// ignore if the type doesn't match
					if (false == recordLevel.equals(configuredLevel))
					{
						Log.d(TAG, "ignoring record, record=" + recordLevel
						        + " configured=" + configuredLevel);
						continue;
					}
					switch (record.getType())
					{
						case PPM:
							// create + store a peak data record
							levelDataRecords.put(record.getChannel(),
							        new PPMLevelDataRecord(record.getChannel(),
							                record.getPeakInDB()));
							break;
						case DIGITALPEAK:
							// create + store a peak data record
							levelDataRecords
							        .put(record.getChannel(),
							                new DigitalPeakLevelDataRecord(
							                        record.getChannel(), record
							                                .getPeakInDB()));
							break;
						case VU:
							levelDataRecords.put(record.getChannel(),
							        new VULevelDataRecord(record.getChannel(),
							                record.getVuInUnits()));

					}
				}
				// swap the existing level data with the new data
				this.levelDataRecords = levelDataRecords;
				// let the listeners know there's new data available
				synchronized (this.listeners)
				{
					for (EventListener listener : listeners)
					{
						listener.notifyLevelsUpdated();
					}
				}
				break;
			default:
				Log.wtf(TAG, "unknown type: "
				        + notification.getType().toString());
				return;
		}
		Log.v(TAG, "ControlV1::handleNotification exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class StateMachineListener implements
	        StateMachine.StateChangeListener<State>
	{

		@Override
		public void notifyStateChange(State state)
		{
			Log.v(TAG,
			        "ControlV1::StateMachineListener::notifyStateChange enter state="
			                + state);
			synchronized (listeners)
			{
				for (EventListener listener : listeners)
				{
					listener.notifyStateChange(state);
				}
			}
			Log.v(TAG,
			        "ControlV1::StateMachineListener::notifyStateChange exit");
		}
	}

	private class LevelChangeMessage implements Runnable
	{
		@Override
		public void run()
		{
			Log.v(TAG, "ControlV1::LevelChangeMessage::run enter");
			stateMachineInstance.evaluate(Event.LEVEL_CHANGE, null);
			Log.v(TAG, "ControlV1::LevelChangeMessage::run exit");
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
			Log.v(TAG, "ControlV1::SPPStateMessage::run enter state=" + state);

			switch (this.state)
			{
				case CONNECTED:
					// send in the event
					stateMachineInstance.evaluate(Event.CONNECTED, null);
					break;
				case DISCONNECTED:
				case RECONNECTING:
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
			Log.v(TAG, "ControlV1::SPPStateMessage::run exit");
		}
	}

	private static class ConnectHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			Log.v(TAG, "ControlV1::ConnectHandler::handleEvent enter object="
			        + object + " data=" + data);
			// send the request for the list of channels
			object.sendQueryChannelRequest();

			// now synchronizing
			Log.v(TAG, "ControlV1::ConnectHandler::handleEvent exit");
			return State.SYNCHRONIZING;

		}
	}

	private static class QueryChannelsResponseHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			Log.v(TAG,
			        "ControlV1::QueryChannelsResponseHandler::handleEvent enter object="
			                + object + " data=" + data);
			V1.QueryAudioChannelsResponse response = (V1.QueryAudioChannelsResponse) data;
			for (int channel : response.getChannelsList())
			{
				// if we don't know about this channel populate
				if (false == object.channelToMeterConfigMapping
				        .containsKey(channel))
				{
					Log.d(TAG, "unknown channel=" + channel
					        + ", setting to NONE");
					// store the channel
					object.channelToMeterConfigMapping.put(channel,
					        new MeterConfig(MeterType.NONE));
				}
				else
				{
					object.sendLevelRequest(channel,
					        object.channelToMeterConfigMapping.get(channel));
				}
			}

			// now connected
			Log.v(TAG,
			        "ControlV1::QueryChannelsResponseHandler::handleEvent exit");
			return State.CONNECTED;
		}
	}

	private static class DisconnectHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			Log.v(TAG,
			        "ControlV1::DisconnectHandler::handleEvent enter object="
			                + object + " data=" + data);
			// clear any level data we may have
			object.levelDataRecords = null;
			// clear any pending messages
			object.pendingRequestQueue.clear();
			// now connecting
			Log.v(TAG, "ControlV1::DisconnectHandler::handleEvent exit");
			return State.CONNECTING;
		}
	}

	private static class ChangeLevelInConnectedHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			Log.v(TAG,
			        "ControlV1::ChangeLevelInConnectedHandler::handleEvent enter object="
			                + object + " data=" + data);
			for (Map.Entry<Integer, MeterConfig> entry : object.channelToMeterConfigMapping
			        .entrySet())
			{
				// send the new level
				object.sendLevelRequest(entry.getKey(), entry.getValue());
			}
			// no change in state
			Log.v(TAG,
			        "ControlV1::ChangeLevelInConnectedHandler::handleEvent exit");
			return null;
		}
	}

}