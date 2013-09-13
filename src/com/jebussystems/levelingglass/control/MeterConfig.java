package com.jebussystems.levelingglass.control;

public class MeterConfig
{
	private final int channel;
	private MeterType metertype;
	private Integer holdtime;

	public MeterConfig(int channel, MeterType metertype)
	{
		this.channel = channel;
		this.metertype = metertype;
	}

	public int getChannel()
	{
		return channel;
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
