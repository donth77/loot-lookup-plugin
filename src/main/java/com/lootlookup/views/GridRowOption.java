package com.lootlookup.views;

public enum GridRowOption {
    FOUR(4),
    FIVE(5);

    private final int value;

    GridRowOption(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
}