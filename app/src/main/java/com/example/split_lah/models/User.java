package com.example.split_lah.models;

import android.os.Parcel;
import android.os.Parcelable;

// User from FireBase
public class User implements Parcelable {
    private String id;
    private String firstName;
    private String lastName;
    private boolean isSelected;
    private String split;
    private String icon;

    public User(String id, String firstName, String lastName, String split, String icon) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.split = split;
        this.icon = icon;
        this.isSelected = false;
    }

    // Required for Parcelable
    protected User(Parcel in) {
        id = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        split = in.readString();
        icon = in.readString();
        isSelected = in.readByte() != 0;
    }

    // Implement Parcelable
    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(split);
        dest.writeString(icon);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getName() { return firstName + " " + lastName; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public String getSplit() { return split; }
    public void setSplit(String split) { this.split = split; }
    public String getIcon() { return icon; }

}
