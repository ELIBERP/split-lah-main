package com.example.split_lah.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualSplit extends Split{
    private int totalUsers;
    private List<String> users;

    public EqualSplit(int userId, List<String> users, double amount) {
        super(userId, amount);
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
    }
}

