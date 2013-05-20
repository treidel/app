package com.jebussystems.levelingglass.control;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import v1.V1;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jebussystems.levelingglass.bluetooth.spp.SPPManager;
import com.jebussystems.levelingglass.bluetooth.spp.SPPMessageHandler;
import com.jebussystems.levelingglass.bluetooth.spp.SPPState;
import com.jebussystems.levelingglass.bluetooth.spp.SPPStateListener;

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

	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "control.v1";

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final SPPManager manager;
	private Level level = Level.NONE;
	private final Collection<LevelListener> listeners = new LinkedList<LevelListener>();
	private Queue<V1.Request> pendingRequestQueue = new LinkedList<V1.Request>();

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

		// send a message to the server
		sendLevelRequest(level);

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

		// when the state changes to connected we trigger the sending of
		// a query for the list of channels
		if (true == state.equals(SPPState.CONNECTED))
		{
			// send the request
			sendQueryChannelRequest();
		}
		else
		{
			// wipe the pending request list
			this.pendingRequestQueue.clear();
		}

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
		ByteBuffer buffer = ByteBuffer.allocate(length + (Short.SIZE / 8));
		// write in the size
		buffer.putShort(length);
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

}
