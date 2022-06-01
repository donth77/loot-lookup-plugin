package com.lootlookup.views;

public enum PriceType
{

	GE(0),
	HA(1),
	NONE(2);

	private final int value;

	PriceType(final int newValue) {
		value = newValue;
	}

	public int getValue() { return value; }
}
