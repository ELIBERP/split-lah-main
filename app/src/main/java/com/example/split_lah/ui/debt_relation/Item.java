package com.example.split_lah.ui.debt_relation;

public class Item {
    private String payer;
    private String payee;
    private String amount;
    private String currency;
    private int iconPayer;
    private int iconPayee;

    public Item(String payer, String payee, String amount, String currency, int iconPayer, int iconPayee) {
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.currency = currency;
        this.iconPayer  = iconPayer;
        this.iconPayee  = iconPayee;
    }
    public String getPayer() {
        return payer;
    }
    public String getPayee() {
        return payee;
    }
    public String getCurrency() {
        return currency;
    }
    public String getAmount() {
        return amount;
    }
    public int getIconPayer() {
        return iconPayer;
    }
    public int getIconPayee() {
        return iconPayee;
    }
    public void setIconPayer(int icon) {
        this.iconPayer = icon;
    }
    public void setIconPayee(int icon) {
        this.iconPayee = icon;
    }
}
