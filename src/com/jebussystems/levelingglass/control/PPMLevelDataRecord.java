package com.jebussystems.levelingglass.control;

public class PPMLevelDataRecord extends LevelDataRecord
{

	private final float peakLevelInDB;

	public PPMLevelDataRecord(int channel, float peakLevelInDB)
	{
		super(channel);
		this.peakLevelInDB = peakLevelInDB;
	}

	public float getPeakLevelInDB()
	{
		return peakLevelInDB;
	}

	@Override
	public MeterType getType()
	{
		return MeterType.PPM;
	}

}
