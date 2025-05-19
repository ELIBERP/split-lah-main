package com.example.split_lah.models;

import com.example.split_lah.R;

public class IconUtils {

    public static int getIconResourceId(String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return R.drawable.giraffe; // Default icon
        }

        switch (iconName.toLowerCase()) {
            case "bear": return R.drawable.bear;
            case "bunny": return R.drawable.bunny;
            case "cat": return R.drawable.cat;
            case "eagle": return R.drawable.eagle;
            case "gorilla": return R.drawable.gorilla;
            case "panda": return R.drawable.panda;
            case "pig": return R.drawable.pig;
            case "transport": return R.drawable.bus;
            case "food": return R.drawable.food;
            case "drink": return R.drawable.drink;
            case "shopping": return R.drawable.shoppingbags;
            case "entertainment": return R.drawable.hotairballoon;
            case "paid": return R.drawable.bank;
            case "utilities": return R.drawable.home;
            case "groceries": return R.drawable.cart;
            case "others": return R.drawable.box;
            default: return R.drawable.ghostbeige;
        }
    }

    public static String getIconNameFromResourceId(int resourceId) {
        if (resourceId == R.drawable.bear) {
            return "bear";
        } else if (resourceId == R.drawable.bunny) {
            return "bunny";
        } else if (resourceId == R.drawable.cat) {
            return "cat";
        } else if (resourceId == R.drawable.eagle) {
            return "eagle";
        } else if (resourceId == R.drawable.gorilla) {
            return "gorilla";
        } else if (resourceId == R.drawable.panda) {
            return "panda";
        } else if (resourceId == R.drawable.pig) {
            return "pig";
        } else if (resourceId == R.drawable.food) {
            return "food";
        } else if (resourceId == R.drawable.drink) {
            return "drink";
        } else if (resourceId == R.drawable.bus) {
            return "transport";
        } else if (resourceId == R.drawable.shoppingbags) {
            return "shopping";
        } else if (resourceId == R.drawable.hotairballoon) {
            return "entertainment";
        } else if (resourceId == R.drawable.bank) {
            return "paid";
        } else if (resourceId == R.drawable.home) {
            return "utilities";
        } else if (resourceId == R.drawable.cart) {
            return "groceries";
        } else if (resourceId == R.drawable.box) {
            return "others";
        }
        else {
            return "ghostbeige";
        }
    }
}