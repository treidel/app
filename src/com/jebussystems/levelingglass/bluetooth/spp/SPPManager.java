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

import com.jebussystems.levelingglass.util.LogWrapper;
import com.jebussystems.levelingglass.util.PoolableMessageManager;
import com.jebussystems.levelingglass.util.StateMachine;

public class SPPManager implements SPPConnection.Listener,
        StateMachine.StateChangeListener<SPPState>
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////
	private static final String TAG = "spp.manager";

	private static final int RETRY_TIMER_IN_SECS = 10;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	public interface CompatibilityReceiver
	{
		void notifyCompatibility(boolean isCompatible);
	}

	enum Event
	{
		CONNECT, DISCONNECT, NOTIFY_CONNECTED, NOTIFY_DISCONNECTED, SENDREQUEST, TIMER
	}

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	private static final StateMachine<SPPState, Event, SPPManager> stateMachine = new StateMachine<SPPState, SPPManager.Event, SPPManager>(
	        SPPManager.Event.class, SPPState.DISCONNECTED);
	private static final PoolableMessageManager messageManager = new PoolableMessageManager();

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final UUID uuid;
	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final ReconnectMessage reconnectMessage = new ReconnectMessage(this);
	private final StateMachine<SPPState, Event, SPPManager>.Instance stateMachineInstance = stateMachine
	        .createInstance(this);
	private SPPMessageHandler messageHandler = null;
	private final Collection<SPPStateListener> listeners = new LinkedList<SPPStateListener>();
	private SPPConnection connection = null;
	private Future<?> timerHandler = null;

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	static
	{
		stateMachine.addHandler(SPPState.DISCONNECTED, Event.CONNECT,
		        new ConnectHandler());
		stateMachine.addHandler(SPPState.DISCONNECTED, Event.DISCONNECT,
		        stateMachine.createDoNothingHandler());
		stateMachine.addHandler(SPPState.DISCONNECTED, Event.SENDREQUEST,
		        stateMachine.createDoNothingHandler());
		stateMachine.addHandler(SPPState.CONNECTING, Event.CONNECT,
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
		stateMachine.addHandler(SPPState.CONNECTED, Event.SENDREQUEST,
		        new MessageHandler());
		stateMachine.addHandler(SPPState.RECONNECTING, Event.TIMER,
		        new ReconnectHandler());
		stateMachine.addHandler(SPPState.RECONNECTING, Event.DISCONNECT,
		        new CancelReconnectHandler());

		// setup the message pools
		messageManager.registerPool(ConnectMessage.class,
		        ConnectMessage.getPoolInstance());
		messageManager.registerPool(DisconnectMessage.class,
		        DisconnectMessage.getPoolInstance());
		messageManager.registerPool(NotifyConnectedMessage.class,
		        NotifyConnectedMessage.getPoolInstance());
		messageManager.registerPool(NotifyDisconnectedMessage.class,
		        NotifyDisconnectedMessage.getPoolInstance());
		messageManager.registerPool(SendRequestMessage.class,
		        SendRequestMessage.getPoolInstance());
	}

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public SPPManager(UUID uuid)
	{
		LogWrapper.v(TAG, "SPPManager::SPPManager enter", "this=", this);

		// store the UUID
		this.uuid = uuid;
		// add ourselves as a state machine listener
		this.stateMachineInstance.setListener(this);

		LogWrapper.v(TAG, "SPPManager::SPPManager exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void setMessageHandler(SPPMessageHandler messageHandler)
	{
		this.messageHandler = messageHandler;
	}

	public boolean connect(BluetoothDevice device)
	{
		LogWrapper.v(TAG, "SPPManager::connect enter", "this=", this,
		        "device=", device);

		// result flag
		boolean result = false;

		// validate that this is a peer'ed device
		if (true == BluetoothAdapter.getDefaultAdapter().getBondedDevices()
		        .contains(device))
		{
			// make sure the peer supports our service
			if (true == checkDeviceForCompatibility(device))
			{
				// make sure we're disconnected
				disconnect();
				// allocate + populate the connection message
				ConnectMessage message = messageManager
				        .allocateMessage(ConnectMessage.class);
				message.init(this, device);
				// queue it to run
				this.executor.execute(message);
				// success
				result = true;
			}
			else
			{
				LogWrapper.w(TAG, "SPP service is not supported on device=",
				        device.getAddress());
			}
		}
		else
		{
			LogWrapper.w(TAG, "device=", device.getAddress(), "is not paired");
		}

		// returning true means we'll at least try to connect
		LogWrapper.v(TAG, "SPPManager::connect exit", "result=", result);
		return result;
	}

	public void disconnect()
	{
		LogWrapper.v(TAG, "SPPManager::disconnect enter", "this=", this);
		// send the disconnect message
		DisconnectMessage message = messageManager
		        .allocateMessage(DisconnectMessage.class);
		this.executor.execute(message);
		LogWrapper.v(TAG, "SPPManager::disconnect exit");
	}

	public void sendRequest(ByteBuffer request)
	{
		LogWrapper.v(TAG, "SPPManager::sendRequest enter", "this=", this,
		        "request=", request);
		SendRequestMessage message = messageManager
		        .allocateMessage(SendRequestMessage.class);
		message.init(this, request);
		this.executor.execute(message);
		LogWrapper.v(TAG, "SPPManager::sendRequest exit");
	}

	public void addSPPStateListener(SPPStateListener listener)
	{
		LogWrapper.v(TAG, "SPPManager::addSPPStateListener enter", "this=",
		        this, "listener=", listener);
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
		LogWrapper.v(TAG, "SPPManager::addSPPStateListener exit");
	}

	public void removeSPPStateListener(SPPStateListener listener)
	{
		LogWrapper.v(TAG, "SPPManager::removeSPPStateListener enter", "this=",
		        this, "listener=", listener);
		synchronized (this.listeners)
		{
			this.listeners.remove(listener);
		}
		LogWrapper.v(TAG, "SPPManager::removeSPPStateListener exit");
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
		LogWrapper.v(TAG, "SPPManager::connected enter", "this=", this,
		        "connection=", connection);
		NotifyConnectedMessage message = messageManager
		        .allocateMessage(NotifyConnectedMessage.class);
		message.init(this, connection);
		this.executor.execute(message);
		LogWrapper.v(TAG, "SPPManager::connected exit");
	}

	@Override
	public void disconnected(SPPConnection connection)
	{
		LogWrapper.v(TAG, "SPPManager::disconnected enter connection="
		        + connection);
		NotifyDisconnectedMessage message = messageManager
		        .allocateMessage(NotifyDisconnectedMessage.class);
		message.init(this, connection);
		this.executor.execute(message);
		LogWrapper.v(TAG, "SPPManager::disconnected exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// StateMachine.StateChangeListener methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void notifyStateChange(SPPState state)
	{
		LogWrapper.v(TAG, "SPPManager::notifyStateChange enter", "this=", this,
		        "state=", state);
		synchronized (listeners)
		{
			for (SPPStateListener listener : listeners)
			{
				listener.notifySPPStateChanged(state);
			}
		}
		LogWrapper.v(TAG, "SPPManager::notifyStateChange exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// package protected methods
	// /////////////////////////////////////////////////////////////////////////

	StateMachine<SPPState, Event, SPPManager>.Instance getStateMachineInstance()
	{
		return stateMachineInstance;
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	private boolean checkDeviceForCompatibility(BluetoothDevice device)
	{
		for (ParcelUuid parcelUUID : device.getUuids())
		{
			if (true == parcelUUID.getUuid().equals(this.uuid))
			{
				return true;
			}
		}
		return false;
	}

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private static class ConnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			LogWrapper.v(TAG, "SPPManager::ConnectHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
			try
			{
				// create the socket
				BluetoothSocket socket = ((BluetoothDevice) data)
				        .createRfcommSocketToServiceRecord(object.uuid);
				// create the connection
				object.connection = new SPPConnection(object, socket,
				        object.messageHandler);

			}
			catch (IOException e)
			{
				LogWrapper.e(TAG, "IOException when creating socket, reason="
				        + e.getMessage());
				return null;
			}

			// now we're connecting
			LogWrapper.v(TAG, "SPPManager::ConnectHandler::handleEvent exit");
			return SPPState.CONNECTING;
		}
	}

	private static class ForceDisconnectHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			LogWrapper.v(TAG,
			        "SPPManager::ForceDisconnectHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
			// close the connection
			object.connection.close();
			object.connection = null;

			// now we're disconnected
			LogWrapper.v(TAG,
			        "SPPManager::ForceDisconnectHandler::handleEvent exit");
			return SPPState.DISCONNECTED;
		}
	}

	private static class NotifyConnectedHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			LogWrapper.v(TAG,
			        "SPPManager::NotifyConnectedHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
			// store the connection
			object.connection = (SPPConnection) data;
			// now we're connected
			LogWrapper.v(TAG,
			        "SPPManager::NotifyConnectedHandler::handleEvent exit");
			return SPPState.CONNECTED;
		}
	}

	private static class NotifyDisconnectedHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			LogWrapper.v(TAG,
			        "SPPManager::NotifyDisconnectedHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
			// close the connection
			object.connection.close();
			object.connection = null;
			// start the reconnect timer using the 'static' reconnect message
			object.reconnectMessage.init(((SPPConnection) data).getDevice());
			object.timerHandler = object.executor.schedule(
			        object.reconnectMessage, RETRY_TIMER_IN_SECS,
			        TimeUnit.SECONDS);
			// now we're reconnecting
			LogWrapper.v(TAG,
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
			LogWrapper.v(TAG,
			        "SPPManager::ReconnectHandler::handleEvent enter", "this=",
			        this, "object=", object, "data=", data);
			// no longer need the timer handle
			object.timerHandler = null;
			try
			{
				// create the socket
				BluetoothSocket socket = ((BluetoothDevice) data)
				        .createRfcommSocketToServiceRecord(object.uuid);
				// trigger a connection
				object.connection = new SPPConnection(object, socket,
				        object.messageHandler);
			}
			catch (IOException e)
			{
				LogWrapper.e(TAG, "IOException when creating socket, reason="
				        + e.getMessage());
				return SPPState.DISCONNECTED;
			}
			// now connecting
			LogWrapper.v(TAG,
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
			LogWrapper.v(TAG,
			        "SPPManager::CancelReconnectHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
			// cancel the timer
			object.timerHandler.cancel(false);
			object.timerHandler = null;

			// now disconnected
			LogWrapper.v(TAG,
			        "SPPManager::CancelReconnectHandler::handleEvent exit");
			return SPPState.DISCONNECTED;
		}
	}

	private static class MessageHandler implements
	        StateMachine.Handler<SPPState, SPPManager>
	{
		@Override
		public SPPState handleEvent(SPPManager object, Object data)
		{
			LogWrapper.v(TAG, "SPPManager::MessageHandler::handleEvent enter",
			        "this=", this, "object=", object, "data=", data);
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
			LogWrapper.v(TAG, "SPPManager::MessageHandler::handleEvent exit",
			        "result=", result);
			return null;
		}
	}
}
