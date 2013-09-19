package com.jebussystems.levelingglass.bluetooth.spp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.jebussystems.levelingglass.util.LogWrapper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class SPPConnection
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////
	private static final String TAG = "spp.connection";

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	public interface Listener
	{
		void connected(SPPConnection connection);

		void disconnected(SPPConnection connection);
	}

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	private final Listener listener;
	private final SPPMessageHandler messageHandler;
	private final BluetoothSocket socket;
	private final DataOutputStream writeStream;
	private final DataInputStream readStream;
	private final Thread thread;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public SPPConnection(Listener listener, BluetoothSocket socket,
	        SPPMessageHandler messageHandler)
	{
		LogWrapper.v(TAG, "SPPConnection::SPPConnection enter", "this=", this,
		        "listener=", listener, "socket=", socket, "messagHandler=",
		        messageHandler);

		// store data and allocate the channels
		this.listener = listener;
		this.socket = socket;
		try
		{
			this.writeStream = new DataOutputStream(socket.getOutputStream());
			this.readStream = new DataInputStream(socket.getInputStream());
		}
		catch (IOException e)
		{
			// never expect this
			LogWrapper.wtf(TAG, "IOException, reason=", e.getMessage());
			throw new IllegalStateException();
		}
		this.messageHandler = messageHandler;
		// spawn a thread to do the connection and read from the channel
		this.thread = new Thread(new ReadThread());
		this.thread.start();

		LogWrapper.v(TAG, "SPPConnection::SPPConnection exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void sendRequest(ByteBuffer request) throws IOException
	{
		LogWrapper.v(TAG, "SPPConnection::sendRequest enter", "this=", this,
		        "request=", request);

		// write the size of the request
		this.writeStream.writeShort(request.capacity());
		// write the request
		this.writeStream.write(request.array());
		// flush
		this.writeStream.flush();

		LogWrapper.v(TAG, "SPPConnection::sendRequest exit");
	}

	public void close()
	{
		LogWrapper.v(TAG, "SPPConnection::close enter", "this=", this);
		// close the socket
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			LogWrapper.e(TAG, e.getMessage());
		}
		// wait for the thread to die
		try
		{
			this.thread.join();
		}
		catch (InterruptedException e)
		{
			LogWrapper.d(TAG, "thread interrupted in join, message=",
			        e.getMessage());
		}
		LogWrapper.v(TAG, "SPPConnection::close exit");
	}

	public BluetoothDevice getDevice()
	{
		return socket.getRemoteDevice();
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
			LogWrapper.v(TAG, "SPPConnection::ReadThread::run enter", "this=",
			        this);

			try
			{
				// actually try to connect
				socket.connect();

				LogWrapper.d(TAG, "connected to device=",
				        socket.getRemoteDevice());

				// notify the listener
				listener.connected(SPPConnection.this);

				// read messages forever
				while (true)
				{
					// read the size of the message
					short length = readStream.readShort();
					// allocate a buffer and read the message in
					ByteBuffer buffer = ByteBuffer.allocate(length);
					readStream.read(buffer.array(), 0, length);
					// call the handler
					messageHandler.handleSPPMessage(buffer);
				}
			}
			catch (IOException e)
			{
				LogWrapper.w(TAG, "IO exception from socket, message=",
				        e.getMessage());
				// notify the listener we've disconnected
				// this will result in the socket being closed in a different
				// thread
				listener.disconnected(SPPConnection.this);
			}
			LogWrapper.v(TAG, "SPPConnection::ReadThread::run exit");
		}
	}
}
