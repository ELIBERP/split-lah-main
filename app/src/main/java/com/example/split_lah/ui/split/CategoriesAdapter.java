package com.example.split_lah.ui.split;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.split_lah.R;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.models.User;

import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final Context context;
    private final ArrayList<String> categories;

    public CategoriesAdapter(Context context, ArrayList<String> categories) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.categories = categories;
    }

    @Override
    public int getCount() {
        return categories != null ? categories.size() : 0;
    }

    @Override
    public String getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    // ---------- collapsed spinner line ----------
    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        CategoriesAdapter.ViewHolder h;
        if (convert == null) {
            convert = inflater.inflate(R.layout.item_paid_by, parent, false);
            h = new CategoriesAdapter.ViewHolder(convert);
            convert.setTag(h);
        } else {
            h = (CategoriesAdapter.ViewHolder) convert.getTag();
        }
        bind(h, categories.get(pos));
        return convert;
    }

    // ---------- each row in the dropdown ----------
    @Override
    public View getDropDownView(int pos, View convert, ViewGroup parent) {
        CategoriesAdapter.ViewHolder h;
        if (convert == null) {
            convert = inflater.inflate(R.layout.item_paid_by, parent, false);
            h = new CategoriesAdapter.ViewHolder(convert);
            convert.setTag(h);
        } else {
            h = (CategoriesAdapter.ViewHolder) convert.getTag();
        }
        bind(h, categories.get(pos));
        return convert;
    }

    // ---------- helper ----------
    private void bind(CategoriesAdapter.ViewHolder h, String category) {
        h.name.setText(category);
        h.icon.setImageResource(IconUtils.getIconResourceId(category));
    }

    private static class ViewHolder {
        final TextView name;
        final ImageView icon;
        ViewHolder(View v) {
            name = v.findViewById(R.id.paid_by_name);
            icon = v.findViewById(R.id.paid_by_icon);
        }
    }
}
