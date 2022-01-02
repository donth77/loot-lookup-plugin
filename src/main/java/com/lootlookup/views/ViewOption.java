package com.lootlookup.views;

public enum ViewOption {
    LIST(0),
    GRID(1);

    private final int value;

    ViewOption(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
}