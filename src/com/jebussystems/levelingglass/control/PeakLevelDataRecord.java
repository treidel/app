package com.jebussystems.levelingglass.control;


public class PeakLevelDataRecord extends LevelDataRecord
{

	private final float peakLevelInDB;

	public PeakLevelDataRecord(int channel, float peakLevelInDB)
	{
		super(channel);
		this.peakLevelInDB = peakLevelInDB;
	}

	public float getPeakLevelInDB()
	{
		return peakLevelInDB;
	}

	@Override
	public Level getType()
	{
		return Level.PEAK;
	}

}
