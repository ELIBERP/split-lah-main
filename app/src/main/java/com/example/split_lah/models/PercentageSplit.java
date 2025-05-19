package com.example.split_lah.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PercentageSplit extends Split {
    private double percentage;
    private int totalUsers;
    private List<String> users;

    public PercentageSplit(int userId, List<String> users, double amount, double percentage) {
        super(userId, amount);
        this.percentage = percentage;
        this.totalUsers = users.size();
        this.users = users;
    }

    @Override
    public HashMap<String, String> calculateShare() {
        HashMap<String, String> splits = new HashMap<>();
        for (String user : users) {
            splits.put(user, String.valueOf(amount / totalUsers));
        }
        return splits;
//        return (amount * percentage) / 100; // Calculates share based on percentage
    }

}
