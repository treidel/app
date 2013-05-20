package com.jebussystems.levelingglass.bluetooth.spp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

public class SPPManager
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////
	private static final String TAG = "spp.manager";
	private static final UUID SERVER_UUID = UUID
	        .fromString("c20d3a1a-6c0d-11e2-aa09-000c298ce626");
	private static final int RECONNECT_PERIOD_IN_SECS = 10;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final ScheduledExecutorService executor = Executors
	        .newSingleThreadScheduledExecutor();
	private final SPPMessageHandler messageHandler;
	private final Collection<SPPStateListener> listeners = new LinkedList<SPPStateListener>();
	private SPPState state = SPPState.DISCONNECTED;
	private SPPConnection connection = null;
	private BluetoothDevice device = null;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public SPPManager(SPPMessageHandler messageHandler)
	{
		// store the message handler
		this.messageHandler = messageHandler;
		// start a periodic timer to do trigger reconnections
		this.executor.scheduleAtFixedRate(new ReconnectMessage(), 0,
		        RECONNECT_PERIOD_IN_SECS, TimeUnit.SECONDS);
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

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
			return false;
		}
		// send the connection message
		ConnectMessage message = new ConnectMessage(device);
		this.executor.execute(message);
		// returning true means we'll at least try to connect
		return true;

	}

	public void disconnect()
	{
		// send the disconnect message
		DisconnectMessage message = new DisconnectMessage();
		this.executor.execute(message);
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
		return state;
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

	private void updateState(SPPState state)
	{
		// update the SPPState
		this.state = state;
		synchronized (listeners)
		{
			for (SPPStateListener listener : listeners)
			{
				listener.notifySPPStateChanged(this.state);
			}
		}
	}

	private boolean isUUIDSupportedByDevice(BluetoothDevice device)
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

	private class ReconnectMessage implements Runnable
	{

		public void run()
		{
			Log.d(TAG, "periodic reconnect starting");
			// if we're already connected don't do anything
			if (true == state.equals(SPPState.CONNECTED))
			{
				Log.d(TAG, "already connected");
				return;
			}
			// if we don't have a device don't do anything
			if (null == device)
			{
				Log.d(TAG, "no device");
				return;
			}
			// if the device stopped being paired remove it
			if (false == BluetoothAdapter.getDefaultAdapter()
			        .getBondedDevices().contains(device))
			{
				Log.d(TAG, "paired device=" + device.toString()
				        + " removed, clearing");
				device = null;
				return;
			}
			// schedule a connection attempt
			executor.submit(new ConnectMessage(device));
		}

	}

	private class ConnectMessage implements Runnable
	{
		private final BluetoothDevice device;

		public ConnectMessage(BluetoothDevice device)
		{
			this.device = device;
		}

		public void run()
		{
			// depending on the state do different things
			switch (state)
			{
				case DISCONNECTED:
					try
					{
						// create the socket
						BluetoothSocket socket = device
						        .createRfcommSocketToServiceRecord(SERVER_UUID);
						// update the state
						updateState(SPPState.CONNECTING);
						// actually try to connect
						socket.connect();
						// create the connection
						connection = new SPPConnection(SPPManager.this, socket,
						        messageHandler);
						// if we get here we are connected so store the
						// device
						SPPManager.this.device = device;
						// update the state
						updateState(SPPState.CONNECTED);

					}
					catch (IOException e)
					{
						Log.w(TAG,
						        "IO exception connecting to: "
						                + device.toString() + ", message="
						                + e.getMessage());
						// paranoia: clean up the connection if it exists
						if (null != connection)
						{
							connection.close();
							connection = null;
						}
						updateState(SPPState.DISCONNECTED);
					}
					break;
				case CONNECTED:
					break;
				default:
					Log.wtf(TAG, "SPPState=" + state.toString());
					return;
			}
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
			try
			{

				// if we don't have a connection then this was not successful
				if (null == connection)
				{
					Log.d(TAG, "no connection, send request not successful");
					return;
				}

				// send the request
				connection.sendRequest(message);
			}
			catch (IOException e)
			{
				Log.d(TAG,
				        "IO exception while sending request, message="
				                + e.getMessage());
				// trigger a disconnect
				disconnect();
			}
		}

	}

	private class DisconnectMessage implements Runnable
	{
		public void run()
		{

			switch (state)
			{
				case CONNECTED:
					// close the connection and forget about it
					connection.close();
					connection = null;
					// update the state
					updateState(SPPState.DISCONNECTED);
					break;
				case DISCONNECTED:
					break;
				default:
					Log.wtf(TAG, "SPPState=" + state.toString());
					return;
			}

		}
	}
}
