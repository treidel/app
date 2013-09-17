package com.jebussystems.levelingglass.control;

public class PeakLevelDataRecord extends LevelDataRecord
{

	private final float peakLevelInDB;
	private final Float holdLevelInDB;

	public PeakLevelDataRecord(int channel, float peakLevelInDB,
	        Float holdLevelInDB)
	{
		super(channel);
		this.peakLevelInDB = peakLevelInDB;
		this.holdLevelInDB = holdLevelInDB;
	}

	public float getPeakLevelInDB()
	{
		return peakLevelInDB;
	}

	public Float getHoldLevelInDB()
	{
		return holdLevelInDB;
	}

	@Override
	public MeterType getType()
	{
		return MeterType.PPM;
	}

}
