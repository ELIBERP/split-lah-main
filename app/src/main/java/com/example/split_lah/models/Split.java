package com.example.split_lah.models;

import java.util.HashMap;

public abstract class Split {
    protected int userId;
    protected double amount;

    public Split(int userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public int getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    // Abstract method to be implemented by subclasses
    public abstract HashMap<String, String> calculateShare();
}
