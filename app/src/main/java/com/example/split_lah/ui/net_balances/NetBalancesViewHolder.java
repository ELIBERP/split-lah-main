package com.example.split_lah.ui.net_balances;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class NetBalancesViewHolder extends RecyclerView.ViewHolder {
    public ShapeableImageView iconMember;
    public TextView member;
    public TextView currency;
    public TextView amount;
    public TextView owedOrOwing;

    public NetBalancesViewHolder(@NonNull View itemView) {
        super(itemView);
        iconMember = itemView.findViewById(R.id.img_net_balances_icon);
        member = itemView.findViewById(R.id.tv_net_balances_member);
        currency = itemView.findViewById(R.id.tv_net_balances_currency);
        amount = itemView.findViewById(R.id.tv_net_balances_amount);
        owedOrOwing = itemView.findViewById(R.id.tv_net_balances_owing_or_owed);
    }
}
