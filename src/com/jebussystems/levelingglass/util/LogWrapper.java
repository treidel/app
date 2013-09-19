package com.jebussystems.levelingglass.util;

import android.util.Log;

public class LogWrapper
{
	private static int logLevel = Log.DEBUG;

	public static void setLogLevel(int logLevel)
	{
		LogWrapper.logLevel = logLevel;
	}

	public static void v(String tag, Object... args)
	{
		// filter log if log level is too low
		if (logLevel > Log.VERBOSE)
		{
			return;
		}
		// generate the log
		String log = concatenateArgs(args);
		Log.wtf(tag, log);
	}

	public static void d(String tag, Object... args)
	{
		// filter log if log level is too low
		if (logLevel > Log.DEBUG)
		{
			return;
		}
		// generate the log
		String log = concatenateArgs(args);
		Log.wtf(tag, log);
	}

	public static void w(String tag, Object... args)
	{
		// filter log if log level is too low
		if (logLevel > Log.WARN)
		{
			return;
		}
		// generate the log
		String log = concatenateArgs(args);
		Log.wtf(tag, log);
	}

	public static void e(String tag, Object... args)
	{
		// filter log if log level is too low
		if (logLevel > Log.ERROR)
		{
			return;
		}
		// generate the log
		String log = concatenateArgs(args);
		Log.wtf(tag, log);
	}

	public static void wtf(String tag, Object... args)
	{
		// generate the log
		String log = concatenateArgs(args);
		Log.wtf(tag, log);
	}
	
	private static String concatenateArgs(Object ... args)
	{
		// create a buffer for the log
		StringBuffer buffer = new StringBuffer();
		// append all args
		for (Object arg : args)
		{
			// output the arg as a string
			buffer.append(arg);
			// only output a space if this isn't a parameter arg
			if ((false == arg instanceof String) || 
				(false == ((String)arg).endsWith("=")))
			{
				buffer.append(' ');
			}			
		}
		return buffer.toString();
	}
}
