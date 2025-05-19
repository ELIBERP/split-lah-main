package com.example.split_lah.ui.net_balances;

public class Item {
    private String member;
    private String amount;
    private String currency;
    private String owedOrOwing;
    private int iconMember;

    public Item(String member, String amount, String currency, String owedOrOwing, int iconMember) {
        this.member = member;
        this.amount = amount;
        this.currency = currency;
        this.owedOrOwing = owedOrOwing;
        this.iconMember = iconMember;
    }

    // Existing getters
    public String getMember() {
        return member;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getOwedOrOwing() {
        return owedOrOwing;
    }

    public int getIconMember() {
        return iconMember;
    }

    // New setters for updating items in-place
    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setOwedOrOwing(String owedOrOwing) {
        this.owedOrOwing = owedOrOwing;
    }

    public void setIconMember(int iconMember) {
        this.iconMember = iconMember;
    }
}