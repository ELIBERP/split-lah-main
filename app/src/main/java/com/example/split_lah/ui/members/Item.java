package com.example.split_lah.ui.members;

public class Item {
    private final String userId;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final String icon;

    public Item(String userId, String firstName, String lastName, String role, String icon) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.icon = icon;
    }

    public String getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public String getIcon() {
        return icon;
    }
}