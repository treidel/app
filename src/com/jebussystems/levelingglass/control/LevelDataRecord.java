package com.jebussystems.levelingglass.control;


public abstract class LevelDataRecord
{
	private final int channel;

	protected LevelDataRecord(int channel)
	{
		this.channel = channel;
	}

	public int getChannel()
	{
		return channel;
	}

	public abstract Level getType();

}
