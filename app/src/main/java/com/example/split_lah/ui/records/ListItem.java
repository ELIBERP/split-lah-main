package com.example.split_lah.ui.records;

public abstract class ListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TRANSACTION = 1;

    abstract public int getType();
}