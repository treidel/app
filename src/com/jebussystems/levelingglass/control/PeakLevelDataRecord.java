package com.jebussystems.levelingglass.control;


public class PeakLevelDataRecord extends LevelDataRecord
{

	private final int peakLevelInDB;

	public PeakLevelDataRecord(int channel, int peakLevelInDB)
	{
		super(channel);
		this.peakLevelInDB = peakLevelInDB;
	}

	public int getPeakLevelInDB()
	{
		return peakLevelInDB;
	}

	@Override
	public Level getType()
	{
		return Level.PEAK;
	}

}
