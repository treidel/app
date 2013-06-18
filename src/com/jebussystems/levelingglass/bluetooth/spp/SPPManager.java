package com.jebussystems.levelingglass.bluetooth.spp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import com.jebussystems.levelingglass.util.StateMachine;

public class SPPManager implements SPPConnection.Listener,
        StateMachine.StateChangeListener<SPPState>
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////
	private static final String TAG = "spp.manager";
	private static final UUID SERVER_UUID = UUID
	        .fromString("c20d3a1a-6c0d-11e2-aa09-000c298ce626");

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	private enum Event
	{
		CONNECT, DISCONNECT, NOTIFY_CONNECTED, NOTIFY_DISCONNECTED, MESSAGE, TIMER
	}

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static final StateMachine<SPPState, Event, SPPManager, Object> stateMachine = new StateMachine<SPPState, SPPManager.Event, SPPManager, Object>(
	        SPPState.DISCONNECTED);

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final StateMachine<SPPState, Event, SPPManager, Object>.Instance stateMachineInstance = stateMachine
	        .createInstance(this);
	private final SPPMessageHandler messageHandler;
	private final Collection<SPPStateListener> listeners = new LinkedList<SPPStateListener>();
	private SPPConnection connection = null;
	private Future<?> timerHandler = null;
	private BluetoothDevice device = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	static
	{
		stateMachine.addHandler(SPPState.DISCONNECTED, Event.CONNECT,
		        new ConnectHandler());
		stateMachine.addHandler(SPPState.DISCONNECTED, Event.DISCONNECT,
		        stateMachine.createDoNothingHandler());
		stateMachine.addHandler(SPPState.CONNECTING, Event.NOTIFY_CONNECTED,
		        new NotifyConnectedHandler());
		stateMachine.addHandler(SPPState.CONNECTING, Event.NOTIFY_DISCONNECTED,
		        new NotifyDisconnectedHandler());
		stateMachine.addHandler(SPPState.CONNECTING, Event.DISCONNECT,
		        new ForceDisconnectHandler());
		stateMachine.addHandler(SPPState.CONNECTED, Event.DISCONNECT,
		        new ForceDisconnectHandler());
		stateMachine.addHandler(SPPState.CONNECTED, Event.NOTIFY_DISCONNECTED,
		        new NotifyDisconnectedHandler());
		stateMachine.addHandler(SPPState.CONNECTED, Event.MESSAGE,
		        new MessageHandler());
		stateMachine.addHandler(SPPState.RECONNECTING, Event.TIMER,
		        new ReconnectHandler());
		stateMachine.addHandler(SPPState.RECONNECTING, Event.DISCONNECT,
		        new CancelReconnectHandler());
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public SPPManager(SPPMessageHandler messageHandler)
	{
		// store the message handler
		this.messageHandler = messageHandler;
		// add ourselves as a state machine listener
		this.stateMachineInstance.addListener(this);
		// TBD: retrieve stored device

		// connect if we have a device
		if (null != this.device)
		{
			ConnectMessage message = new ConnectMessage(this.device);
			this.executor.execute(message);
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public static boolean checkDeviceForCompatibility(BluetoothDevice device)
	{
		// validate that this is a peer'ed device
		if (false == BluetoothAdapter.getDefaultAdapter().getBondedDevices()
		        .contains(device))
		{
			Log.w(TAG, "device=" + device.getAddress() + " is not paired");
			return false;
		}
		// make sure the peer supports our service
		if (false == isUUIDSupportedByDevice(device))
		{
			Log.w(TAG,
			        "SPP service is not supported on device="
			                + device.getAddress());
			return false;
		}
		return true;
	}
	
	public boolean connect(BluetoothDevice device)
	{
		// validate that this is a peer'ed device
		if (false == BluetoothAdapter.getDefaultAdapter().getBondedDevices()
		        .contains(device))
		{
			Log.w(TAG, "device=" + device.getAddress() + " is not paired");
			return false;
		}
		// make sure the peer supports our service
		if (false == isUUIDSupportedByDevice(device))
		{
			Log.w(TAG,
			        "SPP service is not supported on device="
			                + device.getAddress());
			return false;
		}
		// if we already have a device disconnect
		if (null != this.device)
		{
			disconnect();
		}
		// send the connection message
		ConnectMessage message = new ConnectMessage(device);
		this.executor.execute(message);
		// store the device
		this.device = device;
		// TBD: persist the device

		// returning true means we'll at least try to connect
		return true;

	}

	public void disconnect()
	{
		// send the disconnect message
		DisconnectMessage message = new DisconnectMessage();
		this.executor.execute(message);
		// forget about the device
		this.device = null;
		// TBD: clear from persistent storage
	}

	public void sendRequest(ByteBuffer request)
	{
		SendRequestMessage message = new SendRequestMessage(request);
		this.executor.execute(message);
	}

	public void addSPPStateListener(SPPStateListener listener)
	{
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
	}

	public void removeSPPStateListener(SPPStateListener listener)
	{
		synchronized (this.listeners)
		{
			this.listeners.remove(listener);
		}
	}

	public SPPState getState()
	{
		return stateMachineInstance.getState();
	}

	// /////////////////////////////////////////////////////////////////////////
	// SPPConnection.Listener methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void connected(SPPConnection connection)
	{
		NotifyConnectedMessage message = new NotifyConnectedMessage(connection);
		this.executor.execute(message);
	}

	@Override
	public void disconnected(SPPConnection connection)
	{
		NotifyDisconnectedMessage message = new NotifyDisconnectedMessage(
		        connection);
		this.executor.execute(message);
	}

	// /////////////////////////////////////////////////////////////////////////
	// StateMachine.StateChangeListener methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void notifyStateChange(SPPState state)
	{
		synchronized (listeners)
		{
			for (SPPStateListener listener : listeners)
			{
				listener.notifySPPStateChanged(state);
			}
		}
	}
	
	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// package protected methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private static boolean isUUIDSupportedByDevice(BluetoothDevice device)
	{
		// retrieve the UUIds supported by the peer
		if (false == device.fetchUuidsWithSdp())
		{
			Log.w(TAG, "unable to retrieve UUIDs");
			return false;
		}
		// see if our UUID is included
		for (ParcelUuid uuid : device.getUuids())
		{
			if (true == uuid.getUuid().equals(SERVER_UUID))
			{
				return true;
			}
		}

		// not found
		Log.w(TAG, "peer does not support the service UUID=" + SERVER_UUID);
		return false;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ConnectMessage implements Runnable
	{
		private final BluetoothDevice device;

		public ConnectMessage(BluetoothDevice device)
		{
			this.device = device;
		}

		public void run()
		{
			// fire the state machine event
			stateMachineInstance.evaluate(Event.CONNECT, device);
		}
	}

	private class DisconnectMessage implements Runnable
	{
		public void run()
		{
			// run the state machine
			stateMachineInstance.evaluate(Event.DISCONNECT, null);
		}
	}

	private class NotifyConnectedMessage implements Runnable
	{
		private final SPPConnection connection;

		public NotifyConnectedMessage(SPPConnection connection)
		{
			this.connection = connection;
		}

		public void run()
		{
			// ignore if this is stale
			if (this.connection != SPPManager.this.connection)
			{
				Log.w(TAG, "ignoring stale connection");
				this.connection.close();
				return;
			}
			// run the state machine
			stateMachineInstance
			        .evaluate(Event.NOTIFY_CONNECTED, connection);
		}
	}

	private class NotifyDisconnectedMessage implements Runnable
	{
		private final SPPConnection connection;

		public NotifyDisconnectedMessage(SPPConnection connection)
		{
			this.connection = connection;
		}

		public void run()
		{
			// ignore if this is stale
			if (this.connection != SPPManager.this.connection)
			{
				Log.w(TAG, "ignoring stale connection");
				this.connection.close();
				return;
			}
			// run the state machine
			stateMachineInstance.evaluate(Event.NOTIFY_DISCONNECTED,
			        connection.getDevice());
		}
	}

	private class SendRequestMessage implements Runnable
	{
		private final ByteBuffer message;

		public SendRequestMessage(ByteBuffer message)
		{
			this.message = message;
		}

		public void run()
		{
			// run the state machine
			stateMachineInstance.evaluate(Event.MESSAGE, message);
		}
	}

	private class ReconnectMessage implements Runnable
	{
		private final BluetoothDevice device;

		public ReconnectMessage(BluetoothDevice device)
		{
			this.device = device;
		}

		@Override
		public void run()
		{
			stateMachineInstance.evaluate(Event.TIMER, device);
		}
	}

	private static class ConnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			try
			{
				// create the socket
				BluetoothSocket socket = ((BluetoothDevice) data)
				        .createRfcommSocketToServiceRecord(SERVER_UUID);
				// create the connection
				object.connection = new SPPConnection(object, socket,
				        object.messageHandler);

			}
			catch (IOException e)
			{
				Log.e(TAG,
				        "IOException when creating socket, reason="
				                + e.getMessage());
				return null;
			}

			// now we're connecting
			return SPPState.CONNECTING;
		}
	}

	private static class ForceDisconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			// close the connection
			object.connection.close();
			object.connection = null;

			// now we're disconnected
			return SPPState.DISCONNECTED;
		}
	}

	private static class NotifyConnectedHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			// store the connection
			object.connection = (SPPConnection) data;
			// now we're connected
			return SPPState.CONNECTED;
		}
	}

	private static class NotifyDisconnectedHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			// close the connection
			object.connection.close();
			object.connection = null;
			// start the reconnect timer
			ReconnectMessage message = object.new ReconnectMessage(
			        (BluetoothDevice) data);
			object.timerHandler = object.executor.schedule(message, 20,
			        TimeUnit.SECONDS);
			// now we're reconnecting
			return SPPState.RECONNECTING;
		}
	}

	private static class ReconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			// no longer need the timer handle
			object.timerHandler = null;
			try
			{
				// create the socket
				BluetoothSocket socket = ((BluetoothDevice) data)
				        .createRfcommSocketToServiceRecord(SERVER_UUID);
				// trigger a connection
				object.connection = new SPPConnection(object, socket,
				        object.messageHandler);
			}
			catch (IOException e)
			{
				Log.e(TAG,
				        "IOException when creating socket, reason="
				                + e.getMessage());
				return SPPState.DISCONNECTED;
			}
			// now connecting
			return SPPState.CONNECTING;
		}
	}

	private static class CancelReconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			// cancel the timer
			object.timerHandler.cancel(false);
			object.timerHandler = null;

			// now disconnected
			return SPPState.DISCONNECTED;
		}
	}

	private static class MessageHandler implements
	        StateMachine.Handler<SPPState, SPPManager, Object>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			try
			{
				object.connection.sendRequest((ByteBuffer) data);
			}
			catch (IOException e)
			{
				// couldn't send so disconnect
				object.connection.close();
				object.connection = null;
				return SPPState.DISCONNECTED;
			}
			return null;
		}
	}
}
