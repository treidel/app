package com.jebussystems.levelingglass.control;


public class DigitalPeakLevelDataRecord extends LevelDataRecord
{

	private final float peakLevelInDB;

	public DigitalPeakLevelDataRecord(int channel, float peakLevelInDB)
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
		return Level.DIGITALPEAK;
	}

}
