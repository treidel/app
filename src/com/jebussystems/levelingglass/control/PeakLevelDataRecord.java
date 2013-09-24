package com.jebussystems.levelingglass.control;

public class PeakLevelDataRecord extends LevelDataRecord
{

	private float peakLevelInDB;
	private Float holdLevelInDB;

	public PeakLevelDataRecord(int channel)
	{
		super(channel);
	}

	public float getPeakLevelInDB()
	{
		return peakLevelInDB;
	}

	public void setPeaklevelInDB(float peakLevelInDB)
	{
		this.peakLevelInDB = peakLevelInDB;
	}

	public Float getHoldLevelInDB()
	{
		return holdLevelInDB;
	}

	public void setHoldLevelInDB(Float holdLevelInDB)
	{
		this.holdLevelInDB = holdLevelInDB;
	}

	@Override
	public MeterType getType()
	{
		return MeterType.PPM;
	}

}
