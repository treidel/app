package com.jebussystems.levelingglass.control;

public class VULevelDataRecord extends LevelDataRecord
{
	private float vuInUnits;

	public VULevelDataRecord(int channel)
	{
		super(channel);
	}

	public float getVUInUnits()
	{
		return vuInUnits;
	}
	
	public void setVUInUnits(float vuInUnits)
	{
		this.vuInUnits = vuInUnits;
	}

	@Override
	public MeterType getType()
	{
		return MeterType.VU;
	}
}
