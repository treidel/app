package com.jebussystems.levelingglass.control.records;

import com.jebussystems.levelingglass.control.MeterType;

public class DigitalPeakDataRecord extends PeakDataRecord
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// class variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// object variables
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// static initialization
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public DigitalPeakDataRecord(int channel)
	{
		super(channel);
	}

	// /////////////////////////////////////////////////////////////////////////
	// LeveLDataRecord implementation
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public MeterType getType()
	{
		return MeterType.DIGITALPEAK;
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

}