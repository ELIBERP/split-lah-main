package com.example.split_lah.ui.members;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.example.split_lah.models.IconUtils;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersViewHolder> {
    private final Context context;
    private final List<Item> items;

    public MembersAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public MembersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.members_card, parent, false);
        return new MembersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MembersViewHolder holder, int position) {
        Item item = items.get(position);

        // Display user name
        holder.memberName.setText(item.getFirstName() + " " + item.getLastName());

        // Display role
        holder.memberRole.setText(item.getRole());

        // NEW: Display icon
        int iconResId = IconUtils.getIconResourceId(item.getIcon());
        holder.memberIcon.setImageResource(iconResId);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}