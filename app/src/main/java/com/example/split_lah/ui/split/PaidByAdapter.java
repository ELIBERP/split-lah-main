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

import java.util.List;

public class PaidByAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final Context context;
    private final List<User> usersList;

    public PaidByAdapter(Context context, List<User> usersList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.usersList = usersList;
    }

    @Override
    public int getCount() {
        return usersList != null ? usersList.size() : 0;
    }

    @Override
    public User getItem(int position) {
        return usersList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    // ---------- collapsed spinner line ----------
    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        ViewHolder h;
        if (convert == null) {
            convert = inflater.inflate(R.layout.item_paid_by, parent, false);
            h = new ViewHolder(convert);
            convert.setTag(h);
        } else {
            h = (ViewHolder) convert.getTag();
        }
        bind(h, usersList.get(pos));
        return convert;
    }

    // ---------- each row in the dropdown ----------
    @Override
    public View getDropDownView(int pos, View convert, ViewGroup parent) {
        ViewHolder h;
        if (convert == null) {
            convert = inflater.inflate(R.layout.item_paid_by, parent, false);
            h = new ViewHolder(convert);
            convert.setTag(h);
        } else {
            h = (ViewHolder) convert.getTag();
        }
        bind(h, usersList.get(pos));
        return convert;
    }

    // ---------- helper ----------
    private void bind(ViewHolder h, User u) {
        h.name.setText(u.getName());
        h.icon.setImageResource(IconUtils.getIconResourceId(u.getIcon()));
    }

    static class ViewHolder {
        final TextView  name;
        final ImageView icon;
        ViewHolder(View v) {
            name = v.findViewById(R.id.paid_by_name);
            icon = v.findViewById(R.id.paid_by_icon);
        }
    }
}
