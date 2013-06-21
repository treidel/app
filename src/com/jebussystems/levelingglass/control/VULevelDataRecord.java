package com.jebussystems.levelingglass.control;


public class VULevelDataRecord extends LevelDataRecord
{
	private final int powerLevelInDB;

	public VULevelDataRecord(int channel, int powerLevelInDB)
	{
		super(channel);
		this.powerLevelInDB = powerLevelInDB;
	}

	public int getPowerLevelInDB()
	{
		return powerLevelInDB;
	}

	@Override
	public Level getType()
	{
		return Level.VU;
	}
}
