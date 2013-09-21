package com.jebussystems.levelingglass.bluetooth.spp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import com.jebussystems.levelingglass.util.LogWrapper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class SPPConnection
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////
	private static final String TAG = "spp.connection";

	private static final int BUFFER_SIZE = 128;

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

	private static final ObjectPool<ByteBuffer> pool = new StackObjectPool<ByteBuffer>(
	        new ByteBufferFactory());

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

	public static ObjectPool<ByteBuffer> getBufferPool()
	{
		return pool;
	}

	public void sendRequest(ByteBuffer request) throws IOException
	{
		LogWrapper.v(TAG, "SPPConnection::sendRequest enter", "this=", this,
		        "request=", request);

		// write the size of the request
		this.writeStream.writeShort(request.limit());
		// write the request
		this.writeStream.write(request.array());
		// flush
		this.writeStream.flush();
		// release
		try
        {
	        getBufferPool().returnObject(request);
        }
        catch (Exception e)
        {
        	LogWrapper.wtf(TAG, e.getMessage());
        }

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
					try
					{
						ByteBuffer buffer = getBufferPool().borrowObject();
						Assert.assertTrue(buffer.capacity() >= length);
						readStream.read(buffer.array(), 0, length);
						// call the handler
						messageHandler.handleSPPMessage(buffer);
					}
					catch (Exception e)
					{
						Log.wtf(TAG, e.getMessage());
					}
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

	private static class ByteBufferFactory extends
	        BasePoolableObjectFactory<ByteBuffer>
	{

		@Override
		public ByteBuffer makeObject() throws Exception
		{
			LogWrapper.v(TAG,
			        "SPPConnection::ByteBufferFactory::makeObject entry",
			        "this=", this);
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			LogWrapper.v(TAG,
			        "SPPConnection::ByteBufferFactory::makeObject exit",
			        "buffer=", buffer);
			return buffer;
		}

		@Override
		public void passivateObject(ByteBuffer buffer)
		{
			buffer.clear();
		}

	}
}
