package com.example.split_lah.ui.net_balances;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;

import java.util.List;

public class NetBalancesAdapter extends RecyclerView.Adapter<NetBalancesViewHolder> {

    private final Context context;
    private final List<Item> items;

    public NetBalancesAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public NetBalancesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NetBalancesViewHolder(LayoutInflater.from(context).inflate(R.layout.net_balances_card, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull NetBalancesViewHolder holder, int position) {
        Item currentItem = items.get(position);

        holder.member.setText(items.get(position).getMember());
        holder.currency.setText(items.get(position).getCurrency());
        holder.amount.setText(items.get(position).getAmount());
        holder.iconMember.setImageResource(items.get(position).getIconMember());
        holder.owedOrOwing.setText(items.get(position).getOwedOrOwing());

        int textColor;
        String status = currentItem.getOwedOrOwing();
        if ("owes".equals(currentItem.getOwedOrOwing())) {
            textColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark);
        } else if ("is owed".equals(status)) {
            textColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark);
        } else { // "is settled"
            textColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray);
        }


        holder.currency.setTextColor(textColor);
        holder.owedOrOwing.setTextColor(textColor);
        holder.amount.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        // Return 2 or the actual size if less than 2
        return items.size();
    }

    public List<Item> getItems() {
        return items;
    }
}