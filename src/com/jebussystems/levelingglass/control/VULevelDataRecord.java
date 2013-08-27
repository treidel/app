package com.jebussystems.levelingglass.control;


public class VULevelDataRecord extends LevelDataRecord
{
	private final float powerLevelInDB;

	public VULevelDataRecord(int channel, float powerLevelInDB)
	{
		super(channel);
		this.powerLevelInDB = powerLevelInDB;
	}

	public float getPowerLevelInDB()
	{
		return powerLevelInDB;
	}

	@Override
	public Level getType()
	{
		return Level.VU;
	}
}
