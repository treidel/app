package com.jebussystems.levelingglass.control;

public class VULevelDataRecord extends LevelDataRecord
{
	private final float vuInUnits;

	public VULevelDataRecord(int channel, float vuInUnits)
	{
		super(channel);
		this.vuInUnits = vuInUnits;
	}

	public float getVUInUnits()
	{
		return vuInUnits;
	}

	@Override
	public Level getType()
	{
		return Level.VU;
	}
}
