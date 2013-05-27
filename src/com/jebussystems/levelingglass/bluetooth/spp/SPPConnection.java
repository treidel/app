package com.jebussystems.levelingglass.bluetooth.spp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class SPPConnection
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////
	private static final String TAG = "spp.connection";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final SPPManager manager;
	private final SPPMessageHandler messageHandler;
	private final BluetoothSocket socket;
	private final DataOutputStream writeStream;
	private final DataInputStream readStream;
	private final Thread thread;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public SPPConnection(SPPManager manager, BluetoothSocket socket,
	        SPPMessageHandler messageHandler) throws IOException
	{
		// store data and allocate the channels
		this.manager = manager;
		this.socket = socket;
		this.writeStream = new DataOutputStream(socket.getOutputStream());
		this.readStream = new DataInputStream(socket.getInputStream());
		this.messageHandler = messageHandler;
		// spawn a thread to read from the channel
		this.thread = new Thread(new ReadThread());
		this.thread.start();
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void sendRequest(ByteBuffer request) throws IOException
	{
		// write the size of the request
		this.writeStream.writeShort(request.capacity());
		// write the request
		this.writeStream.write(request.array());
		// flush
		this.writeStream.flush();
	}

	public void close()
	{
		// close the socket
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
		}
		// wait for the thread to die
		try
		{
			this.thread.join();
		}
		catch (InterruptedException e)
		{
			Log.d(TAG, "thread interrupted in join, message=" + e.getMessage());
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// inner classes
	// /////////////////////////////////////////////////////////////////////////

	private class ReadThread implements Runnable
	{

		public void run()
		{

			{
				try
				{
					// read the size of the message
					short length = readStream.readShort();
					// allocate a buffer and read the message in
					ByteBuffer buffer = ByteBuffer.allocate(length);
					readStream.read(buffer.array(), 0, length);
					// call the handler
					messageHandler.handleSPPMessage(buffer);
				}
				catch (IOException e)
				{
					Log.w(TAG,
					        "IO exception from socket, message="
					                + e.getMessage());
					// notify the manager to disconnect
					// this will result in the socket being closed in a different thread
					manager.disconnect();
				}
			}
		}

	}

}
