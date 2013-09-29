package com.jebussystems.levelingglass.control.v1;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Assert;
import v1.V1;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jebussystems.levelingglass.app.LevelingGlassApplication;
import com.jebussystems.levelingglass.bluetooth.spp.SPPConnection;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.bluetooth.spp.SPPMessageHandler;
import com.jebussystems.levelingglass.bluetooth.spp.SPPState;
import com.jebussystems.levelingglass.bluetooth.spp.SPPStateListener;
import com.jebussystems.levelingglass.control.MeterType;
import com.jebussystems.levelingglass.control.config.HoldTimeConfig;
import com.jebussystems.levelingglass.control.config.MeterConfig;
import com.jebussystems.levelingglass.control.config.MeterConfigFactory;
import com.jebussystems.levelingglass.control.records.LevelDataRecord;
import com.jebussystems.levelingglass.control.records.LevelDataRecordFactory;
import com.jebussystems.levelingglass.control.records.PeakDataRecord;
import com.jebussystems.levelingglass.util.EnumMapper;
import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessageManager;
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

	enum Event
	{

		CONNECTED, DISCONNECTED, QUERY_CHANNELS_RESPONSE, LEVEL_CHANGE
	}

	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "control.v1";
	public static final UUID SERVER_UUID = UUID
	        .fromString("c20d3a1a-6c0d-11e2-aa09-000c298ce626");

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static final EnumMapper<MeterType, V1.LevelType> levelMapper = new EnumMapper<MeterType, V1.LevelType>(
	        MeterType.class, V1.LevelType.class);
	private static final StateMachine<State, Event, ControlV1> stateMachine = new StateMachine<State, Event, ControlV1>(
	        Event.class, State.CONNECTING);
	private static final PoolableMessageManager messageManager = new PoolableMessageManager();

	// singleton
	private static ControlV1 instance = null;

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final SPPManager sppManager = new SPPManager(SERVER_UUID);
	private final Collection<EventListener> listeners = new LinkedList<EventListener>();
	private Queue<V1.Request> pendingRequestQueue = new LinkedList<V1.Request>();
	private StateMachine<State, Event, ControlV1>.Instance stateMachineInstance = stateMachine
	        .createInstance(this);
	private final Map<Integer, LevelDataRecord> levelDataRecords = new ConcurrentHashMap<Integer, LevelDataRecord>();;

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

		// register our message pools
		messageManager.registerPool(LevelChangeMessage.class,
		        LevelChangeMessage.getPoolInstance());
		messageManager.registerPool(SPPStateMessage.class,
		        SPPStateMessage.getPoolInstance());
		messageManager.registerPool(SPPResponseMessage.class,
		        SPPResponseMessage.getPoolInstance());
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	private ControlV1()
	{
		LogWrapper.v(TAG, "ControlV1::ControlV1 enter", "this=", this);

		// register as the SPP message handler
		this.sppManager.setMessageHandler(this);
		// we also care about state transitions
		this.sppManager.addSPPStateListener(this);

		// setup the state machine state change listener
		this.stateMachineInstance.setListener(new StateMachineListener());

		LogWrapper.v(TAG, "ControlV1::ControlV1 exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static ControlV1 getInstance()
	{
		synchronized (ControlV1.class)
		{
			if (null == instance)
			{
				instance = new ControlV1();
			}
		}
		return instance;
	}

	public void notifyLevelConfigChange()
	{
		LogWrapper.v(TAG, "ControlV1::notifyLevelConfigChange enter", "this=",
		        this);

		// clear all level data
		this.levelDataRecords.clear();

		// trigger the state machine
		try
		{
			LevelChangeMessage message = messageManager
			        .allocateMessage(LevelChangeMessage.class);
			this.executor.execute(message);
		}
		catch (Exception e)
		{
			LogWrapper.wtf(TAG, e.getMessage());
		}

		LogWrapper.v(TAG, "ControlV1::updateLevel exit");
	}

	public void addListener(EventListener listener)
	{
		LogWrapper.v(TAG, "ControlV1::addListener enter", "this=", this,
		        "listener=" + listener);
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
		LogWrapper.v(TAG, "ControlV1::addListener exit");
	}

	public void removeListener(EventListener listener)
	{
		LogWrapper.v(TAG, "ControlV1::removeListener enter", "this=", this,
		        "listener=", listener);
		synchronized (this.listeners)
		{
			this.listeners.remove(listener);
		}
		LogWrapper.v(TAG, "ControlV1::removeListener exit");
	}

	public Map<Integer, LevelDataRecord> getLevelDataRecord()
	{
		return levelDataRecords;
	}

	public SPPManager getManager()
	{
		return sppManager;
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
		LogWrapper.v(TAG, "ControlV1::notifySPPStateChanged enter", "this=",
		        this, "state=" + state);

		// send ourselves a message to handle this
		try
		{
			SPPStateMessage message = messageManager
			        .allocateMessage(SPPStateMessage.class);
			message.init(state);
			this.executor.execute(message);
		}
		catch (Exception e)
		{
			LogWrapper.wtf(TAG, e.getMessage());
		}

		LogWrapper.v(TAG, "ControlV1::notifySPPStateChanged exit");
	}

	// ////////////////////////////////////////////////////////////////////////
	// SPPMessageHandler implementation
	// ////////////////////////////////////////////////////////////////////////

	public void handleSPPMessage(ByteBuffer message)
	{
		LogWrapper.v(TAG, "ControlV1::handleSPPMessage enter", "this=", this,
		        "message=", message);

		try
		{
			// slice up the data into a form protobuf can parse
			ByteString data = ByteString.copyFrom(message.array(), 0,
			        message.limit());
			// decode the message
			V1.ResponseOrNotification msg = V1.ResponseOrNotification
			        .parseFrom(data);
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
					LogWrapper.e(TAG, "unknown type=", msg.getType());
					return;
			}
		}
		catch (InvalidProtocolBufferException e)
		{
			LogWrapper.e(TAG, "unable to decode message, reason=",
			        e.getMessage());
		}
		LogWrapper.v(TAG, "ControlV1::handleSPPMessage exit");
	}

	// ////////////////////////////////////////////////////////////////////////
	// package protected method implementations
	// ////////////////////////////////////////////////////////////////////////

	StateMachine<State, Event, ControlV1>.Instance getStateMachineInstance()
	{
		return stateMachineInstance;
	}

	// ////////////////////////////////////////////////////////////////////////
	// private method implementations
	// ////////////////////////////////////////////////////////////////////////

	private void sendLevelRequest(MeterConfig config)
	{
		LogWrapper.v(TAG, "ControlV1::sendLevelRequest enter", "this=", this,
		        "config=", config);

		// remove any existing data record
		this.levelDataRecords.remove(config.getChannel());

		// create + store the new record
		LevelDataRecord record = LevelDataRecordFactory.createLevelDataRecord(
		        config.getMeterType(), config.getChannel());
		if (null != record)
		{
			this.levelDataRecords.put(config.getChannel(), record);
		}

		// build the message to set the level
		V1.SetLevelRequest.Builder setLevelRequestBuilder = V1.SetLevelRequest
		        .newBuilder();
		setLevelRequestBuilder.setType(levelMapper.mapToExternal(config
		        .getMeterType()));
		setLevelRequestBuilder.setChannel(config.getChannel());
		// only set the hold time if this meter supports it
		if (true == config instanceof HoldTimeConfig)
		{
			setLevelRequestBuilder.setHoldtime(((HoldTimeConfig) config)
			        .getHoldtime());
		}
		V1.Request.Builder requestBuilder = V1.Request.newBuilder();
		requestBuilder.setSetlevel(setLevelRequestBuilder);
		requestBuilder.setType(V1.RequestType.SETLEVEL);

		// send the message
		sendRequest(requestBuilder.build());

		LogWrapper.v(TAG, "ControlV1::sendLevelRequest exit");
	}

	private void sendQueryChannelRequest()
	{
		LogWrapper.v(TAG, "ControlV1::sendQueryChannelRequest enter", "this=",
		        this);

		// build the message to set the level
		V1.QueryAudioChannelsRequest.Builder queryChannelRequestBuilder = V1.QueryAudioChannelsRequest
		        .newBuilder();
		V1.Request.Builder requestBuilder = V1.Request.newBuilder();
		requestBuilder.setQueryaudiochannels(queryChannelRequestBuilder);
		requestBuilder.setType(V1.RequestType.QUERYAUDIOCHANNELS);

		// send the message
		sendRequest(requestBuilder.build());

		LogWrapper.v(TAG, "ControlV1::sendQueryChannelRequest exit");
	}

	private void sendRequest(V1.Request request)
	{
		LogWrapper.v(TAG, "ControlV1::sendRequest", "this=", this, "request=",
		        request);
		// add the request to the bottom of the pending list
		this.pendingRequestQueue.add(request);
		// calculate how large the request is in bytes
		short length = (short) request.getSerializedSize();
		try
		{
			// allocate a byte buffer to hold it
			ByteBuffer buffer = SPPConnection.getBufferPool().borrowObject();
			Assert.assertTrue(buffer.capacity() >= length);
			// wrap the buffer
			CodedOutputStream stream = CodedOutputStream.newInstance(buffer
			        .array());
			// write in the serialized request
			request.writeTo(stream);
			// indicate the number of bytes used
			buffer.limit(length);
			// off she goes
			this.sppManager.sendRequest(buffer);
		}
		catch (Exception e)
		{
			LogWrapper.wtf(TAG, e.getMessage());
		}

		LogWrapper.v(TAG, "ControlV1::sendRequest exit");

	}

	private void handleResponse(V1.Response response)
	{
		LogWrapper.v(TAG, "ControlV1::handleResponse enter", "this=", this,
		        "response=", response);

		// if there are no pending request we have a problem
		if (true == this.pendingRequestQueue.isEmpty())
		{
			LogWrapper.e(TAG, "response received when request list is empty");
			this.sppManager.disconnect();
			return;
		}

		// get the top of the request list
		V1.Request request = this.pendingRequestQueue.remove();

		// request + response should match up
		if (false == request.getType().equals(response.getType()))
		{
			LogWrapper.e(TAG, "request + response types don't match");
			this.sppManager.disconnect();
			return;
		}

		// the server should never say no
		if (true != response.getSuccess())
		{
			LogWrapper.w(TAG,
			        "server rejected request, resetting bluetooth connection");
			// force the server to disconnect
			this.sppManager.disconnect();
		}
		else
		{
			// fire a message to process in our own thread
			SPPResponseMessage message = messageManager
			        .allocateMessage(SPPResponseMessage.class);
			message.init(response);
			this.executor.execute(message);
		}
		LogWrapper.v(TAG, "ControlV1::handleResponse exit");
	}

	private void handleNotification(V1.Notification notification)
	{
		LogWrapper.v(TAG, "ControlV1::handleNotification enter", "this=", this,
		        "notification=", notification.toString());
		switch (notification.getType())
		{
			case LEVEL:
				for (v1.V1.LevelRecord externalRecord : notification.getLevel()
				        .getRecordsList())

				{
					// convert the type into our internal version
					MeterType recordLevel = levelMapper
					        .mapToInternal(externalRecord.getType());
					// get the configured level
					MeterType configuredLevel = LevelingGlassApplication
					        .getInstance()
					        .getConfigForChannel(externalRecord.getChannel())
					        .getMeterType();
					// ignore if the type doesn't match
					if (false == recordLevel.equals(configuredLevel))
					{
						LogWrapper.d(TAG, "ignoring record, record=",
						        recordLevel, "configured=", configuredLevel);
						continue;
					}
					// get the record
					LevelDataRecord internalRecord = this.levelDataRecords
					        .get(externalRecord.getChannel());
					if (null == internalRecord)
					{
						LogWrapper.d(TAG, "internal record missing, channel=",
						        externalRecord.getChannel());
						continue;
					}
					synchronized (internalRecord)
					{
						switch (configuredLevel)
						{
							case PPM:
							case DIGITALPEAK:
								// update the internal record
								internalRecord.setLevel(externalRecord
								        .getPeakInDB());
								((PeakDataRecord) internalRecord)
								        .setHold(externalRecord.getHoldInDB());
								break;
							case VU:
								internalRecord.setLevel(externalRecord
								        .getVuInUnits());
								break;
						}
					}
				}
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
				LogWrapper.wtf(TAG, "unknown type=", notification.getType());
				return;
		}
		LogWrapper.v(TAG, "ControlV1::handleNotification exit");
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
			LogWrapper.v(TAG,
			        "ControlV1::StateMachineListener::notifyStateChange enter",
			        "this=", this, "state=", state);
			synchronized (listeners)
			{
				for (EventListener listener : listeners)
				{
					listener.notifyStateChange(state);
				}
			}
			LogWrapper.v(TAG,
			        "ControlV1::StateMachineListener::notifyStateChange exit");
		}
	}

	private static class ConnectHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			LogWrapper.v(TAG, "ControlV1::ConnectHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
			// send the request for the list of channels
			object.sendQueryChannelRequest();

			// now synchronizing
			LogWrapper.v(TAG, "ControlV1::ConnectHandler::handleEvent exit");
			return State.SYNCHRONIZING;

		}
	}

	private static class QueryChannelsResponseHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			LogWrapper
			        .v(TAG,
			                "ControlV1::QueryChannelsResponseHandler::handleEvent enter",
			                "this=", this, "object=", object, "data=", data);
			V1.QueryAudioChannelsResponse response = (V1.QueryAudioChannelsResponse) data;
			for (int channel : response.getChannelsList())
			{
				// if we don't know about this channel populate
				MeterConfig config = LevelingGlassApplication.getInstance()
				        .getConfigForChannel(channel);
				if (null == config)
				{
					LogWrapper.d(TAG, "unknown channel=", channel,
					        ", setting to NONE");
					// create + store the channel config
					config = MeterConfigFactory.createMeterConfig(
					        MeterType.NONE, channel);
					LevelingGlassApplication.getInstance().setConfigForChannel(
					        config);
				}
				else
				{
					LogWrapper.d(TAG, "updating channel=", channel);
					object.sendLevelRequest(config);
				}
			}

			// now connected
			LogWrapper
			        .v(TAG,
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
			LogWrapper.v(TAG,
			        "ControlV1::DisconnectHandler::handleEvent enter", "this=",
			        this, "object=", object, "data=" + data);
			// clear any level data we may have
			object.levelDataRecords.clear();
			// clear any pending messages
			object.pendingRequestQueue.clear();
			// now connecting
			LogWrapper.v(TAG, "ControlV1::DisconnectHandler::handleEvent exit");
			return State.CONNECTING;
		}
	}

	private static class ChangeLevelInConnectedHandler implements
	        StateMachine.Handler<State, ControlV1>
	{
		@Override
		public State handleEvent(ControlV1 object, Object data)
		{
			LogWrapper
			        .v(TAG,
			                "ControlV1::ChangeLevelInConnectedHandler::handleEvent enter",
			                "this=", this, "object=", object, "data=" + data);
			for (int channel : LevelingGlassApplication.getInstance()
			        .getChannelSet())
			{
				// get the config
				MeterConfig config = LevelingGlassApplication.getInstance()
				        .getConfigForChannel(channel);
				// send the new level
				object.sendLevelRequest(config);
			}
			// no change in state
			LogWrapper
			        .v(TAG,
			                "ControlV1::ChangeLevelInConnectedHandler::handleEvent exit");
			return null;
		}
	}

}