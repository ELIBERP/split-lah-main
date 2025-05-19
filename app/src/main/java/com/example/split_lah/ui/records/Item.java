package com.example.split_lah.ui.records;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Item extends ListItem{
    private String title;
    private String date;
    private String amount;
    private String currency;
    private int icon;
    private List<Integer> memberIcons;
    private final String payer;
    private String id;

    /**
     * Constructs a new Item with the given details.
     * 
     * @param title The title of the item.
     * @param date The date of the item.
     * @param amount The amount associated with the item.
     * @param currency The currency of the amount.
     * @param icon The icon associated with the item.
     * @param memberIcons A list of icons representing members.
     */
    public Item(String id, String title, String date, String amount, String currency,
                int icon, List<Integer> memberIcons, String payer) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.currency = currency;
        this.icon = icon;
        this.memberIcons = memberIcons;
        this.payer = payer;
    }

    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public List<Integer> getMemberIcons() {
        return memberIcons;
    }

    public void setMemberIcons(List<Integer> memberIcons) {
        this.memberIcons = memberIcons;
    }

    @Override
    public int getType() {
        return TYPE_TRANSACTION;
    }

    // Add methods to parse the date
    public Date getDateObject() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            return format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }

    public String getMonth() {
        SimpleDateFormat format = new SimpleDateFormat("MMMM", Locale.US);
        return format.format(getDateObject());
    }

    public String getYear() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy", Locale.US);
        return format.format(getDateObject());
    }

    // Add getters to extract month and year as strings
    public String getMonthYearKey() {
        return getMonth() + " " + getYear();
    }

    public String getPayer() {
        return payer;
    }
}

