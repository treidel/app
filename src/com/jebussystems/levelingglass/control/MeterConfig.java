package com.jebussystems.levelingglass.control;

public class MeterConfig
{
	private MeterType metertype;
	private Integer holdtime;
	
	public MeterConfig(MeterType metertype)
	{
		this.metertype = metertype;
	}
	
	public MeterType getMeterType()
    {
	    return metertype;
    }

	public void setMeterType(MeterType metertype)
    {
	    this.metertype = metertype;
    }

	public Integer getHoldtime()
    {
	    return holdtime;
    }

	public void setHoldtime(Integer holdtime)
    {
	    this.holdtime = holdtime;
    }
	
	
	
}
