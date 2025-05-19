package com.example.split_lah.ui.records;

public class HeaderItem extends ListItem {
    private String month;
    private String year;

    public HeaderItem(String month, String year) {
        this.month = month;
        this.year = year;
    }

    public String getMonthYear() {
        return month + " " + year;
    }

    @Override
    public int getType() {
        return TYPE_HEADER;
    }
}