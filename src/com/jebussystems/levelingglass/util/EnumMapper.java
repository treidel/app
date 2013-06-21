package com.jebussystems.levelingglass.util;

import java.util.EnumMap;
import java.util.Map;

public class EnumMapper<I extends Enum<I>, E extends Enum<E>>
{

	private final Map<I, E> internalToExternal;
	private final Map<E, I> externalToInternal;

	public EnumMapper(Class<I> internalClass, Class<E> externalClass)
	{
		this.internalToExternal = new EnumMap<I, E>(internalClass);
		this.externalToInternal = new EnumMap<E, I>(externalClass);
	}

	public void addMapping(I internal, E external)
	{
		this.internalToExternal.put(internal, external);
		this.externalToInternal.put(external, internal);
	}

	public E mapToExternal(I value)
	{
		return internalToExternal.get(value);
	}

	public I mapToInternal(E value)
	{
		return externalToInternal.get(value);
	}
}
