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

	public static final UUID SERVER_UUID = UUID
	        .fromString("c20d3a1a-6c0d-11e2-aa09-000c298ce626");

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	public interface CompatibilityReceiver
	{
		void notifyCompatibility(boolean isCompatible);
	}

	private enum Event
	{
		CONNECT, DISCONNECT, NOTIFY_CONNECTED, NOTIFY_DISCONNECTED, MESSAGE, TIMER
	}

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static final StateMachine<SPPState, Event, SPPManager> stateMachine = new StateMachine<SPPState, SPPManager.Event, SPPManager>(
	        SPPManager.Event.class, SPPState.DISCONNECTED);

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final StateMachine<SPPState, Event, SPPManager>.Instance stateMachineInstance = stateMachine
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
		Log.v(TAG, "SPPManager::SPPManager enter messageHandler="
		        + messageHandler);
		// store the message handler
		this.messageHandler = messageHandler;
		// add ourselves as a state machine listener
		this.stateMachineInstance.setListener(this);
		Log.v(TAG, "SPPManager::SPPManager exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public boolean connect(BluetoothDevice device)
	{
		Log.v(TAG, "SPPManager::connect enter device=" + device);

		// validate that this is a peer'ed device
		if (false == BluetoothAdapter.getDefaultAdapter().getBondedDevices()
		        .contains(device))
		{
			Log.w(TAG, "device=" + device.getAddress() + " is not paired");
			return false;
		}

		// make sure the peer supports our service
		if (false == checkDeviceForCompatibility(device))
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

		// returning true means we'll at least try to connect
		Log.v(TAG, "SPPManager::connect exit");
		return true;

	}

	public void disconnect()
	{
		Log.v(TAG, "SPPManager::disconnect enter");
		// send the disconnect message
		DisconnectMessage message = new DisconnectMessage();
		this.executor.execute(message);
		// forget about the device
		this.device = null;
		Log.v(TAG, "SPPManager::disconnect exit");
	}

	public void sendRequest(ByteBuffer request)
	{
		Log.v(TAG, "SPPManager::sendRequest enter request=" + request);
		SendRequestMessage message = new SendRequestMessage(request);
		this.executor.execute(message);
		Log.v(TAG, "SPPManager::sendRequest exit");
	}

	public void addSPPStateListener(SPPStateListener listener)
	{
		Log.v(TAG, "SPPManager::addSPPStateListener enter listener=" + listener);
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
		Log.v(TAG, "SPPManager::addSPPStateListener exit");
	}

	public void removeSPPStateListener(SPPStateListener listener)
	{
		Log.v(TAG, "SPPManager::removeSPPStateListener enter listener="
		        + listener);
		synchronized (this.listeners)
		{
			this.listeners.remove(listener);
		}
		Log.v(TAG, "SPPManager::removeSPPStateListener exit");
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
		Log.v(TAG, "SPPManager::connected enter connection=" + connection);
		NotifyConnectedMessage message = new NotifyConnectedMessage(connection);
		this.executor.execute(message);
		Log.v(TAG, "SPPManager::connected exit");
	}

	@Override
	public void disconnected(SPPConnection connection)
	{
		Log.v(TAG, "SPPManager::disconnected enter connection=" + connection);
		NotifyDisconnectedMessage message = new NotifyDisconnectedMessage(
		        connection);
		this.executor.execute(message);
		Log.v(TAG, "SPPManager::disconnected exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// StateMachine.StateChangeListener methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void notifyStateChange(SPPState state)
	{
		Log.v(TAG, "SPPManager::notifyStateChange enter state=" + state);
		synchronized (listeners)
		{
			for (SPPStateListener listener : listeners)
			{
				listener.notifySPPStateChanged(state);
			}
		}
		Log.v(TAG, "SPPManager::notifyStateChange exit");
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

	private boolean checkDeviceForCompatibility(BluetoothDevice device)
	{
		for (ParcelUuid parcelUUID : device.getUuids())
		{
			if (true == parcelUUID.getUuid().equals(SERVER_UUID))
			{
				return true;
			}
		}
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
			Log.v(TAG, "SPPManager::ConnectMessage::run enter device=" + device);
			// fire the state machine event
			stateMachineInstance.evaluate(Event.CONNECT, device);
			Log.v(TAG, "SPPManager::ConnectMessage::run exit");
		}
	}

	private class DisconnectMessage implements Runnable
	{
		public void run()
		{
			Log.v(TAG, "SPPManager::DisconnectMessage::run enter");
			// run the state machine
			stateMachineInstance.evaluate(Event.DISCONNECT, null);
			Log.v(TAG, "SPPManager::DisconnectMessage::run exit");
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
			Log.v(TAG,
			        "SPPManager::NotifyConnectedMessage::run enter connection="
			                + connection);
			// ignore if this is stale
			if (this.connection != SPPManager.this.connection)
			{
				Log.w(TAG, "connect: ignoring stale connection for");
				this.connection.close();
				return;
			}
			// run the state machine
			stateMachineInstance.evaluate(Event.NOTIFY_CONNECTED, connection);
			Log.v(TAG, "SPPManager::NotifyConnectedMessage::run exit");
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
			Log.v(TAG,
			        "SPPManager::NotifyDisconnectedMessage::run enter connection="
			                + connection);
			// ignore if this is stale
			if (this.connection != SPPManager.this.connection)
			{
				Log.w(TAG, "disconnect: ignoring stale connection");
				this.connection.close();
				return;
			}
			// run the state machine
			stateMachineInstance.evaluate(Event.NOTIFY_DISCONNECTED,
			        connection.getDevice());

			Log.v(TAG, "SPPManager::NotifyDisconnectedMessage::run exit");
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
			Log.v(TAG, "SPPManager::SendRequestMessage::run enter message="
			        + message);
			// run the state machine
			stateMachineInstance.evaluate(Event.MESSAGE, message);
			Log.v(TAG, "SPPManager::SendRequestMessage::run exit");
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
			Log.v(TAG, "SPPManager::ReconnectMessage::run enter device="
			        + device);
			stateMachineInstance.evaluate(Event.TIMER, device);
			Log.v(TAG, "SPPManager::ReconnectMessage::run exit");
		}
	}

	private static class ConnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG, "SPPManager::ConnectHandler::handleEvent enter object="
			        + object + " data=" + data);
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
			Log.v(TAG, "SPPManager::ConnectHandler::handleEvent exit");
			return SPPState.CONNECTING;
		}
	}

	private static class ForceDisconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG,
			        "SPPManager::ForceDisconnectHandler::handleEvent enter object="
			                + object + " data=" + data);
			// close the connection
			object.connection.close();
			object.connection = null;

			// now we're disconnected
			Log.v(TAG, "SPPManager::ForceDisconnectHandler::handleEvent exit");
			return SPPState.DISCONNECTED;
		}
	}

	private static class NotifyConnectedHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG,
			        "SPPManager::NotifyConnectedHandler::handleEvent enter object="
			                + object + " data=" + data);
			// store the connection
			object.connection = (SPPConnection) data;
			// now we're connected
			Log.v(TAG, "SPPManager::NotifyConnectedHandler::handleEvent exit");
			return SPPState.CONNECTED;
		}
	}

	private static class NotifyDisconnectedHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG,
			        "SPPManager::NotifyDisconnectedHandler::handleEvent enter object="
			                + object + " data=" + data);
			// close the connection
			object.connection.close();
			object.connection = null;
			// start the reconnect timer
			ReconnectMessage message = object.new ReconnectMessage(
			        (BluetoothDevice) data);
			object.timerHandler = object.executor.schedule(message, 20,
			        TimeUnit.SECONDS);
			// now we're reconnecting
			Log.v(TAG,
			        "SPPManager::NotifyDisconnectedHandler::handleEvent exit");
			return SPPState.RECONNECTING;
		}
	}

	private static class ReconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG,
			        "SPPManager::NotifyDisconnectedHandler::handleEvent enter object="
			                + object + " data=" + data);
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
			Log.v(TAG,
			        "SPPManager::NotifyDisconnectedHandler::handleEvent exit");
			return SPPState.CONNECTING;
		}
	}

	private static class CancelReconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG,
			        "SPPManager::CancelReconnectHandler::handleEvent enter object="
			                + object + " data=" + data);
			// cancel the timer
			object.timerHandler.cancel(false);
			object.timerHandler = null;

			// now disconnected
			Log.v(TAG, "SPPManager::CancelReconnectHandler::handleEvent exit");
			return SPPState.DISCONNECTED;
		}
	}

	private static class MessageHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			Log.v(TAG, "SPPManager::MessageHandler::handleEvent enter object="
			        + object + " data=" + data);
			// assume we don't want to change state
			SPPState result = null;
			
			try
			{
				object.connection.sendRequest((ByteBuffer) data);
			}
			catch (IOException e)
			{
				// couldn't send so disconnect
				object.connection.close();
				object.connection = null;
				result = SPPState.DISCONNECTED;
			}
			Log.v(TAG, "SPPManager::MessageHandler::handleEvent exit result=" + result);
			return null;
		}
	}
}
