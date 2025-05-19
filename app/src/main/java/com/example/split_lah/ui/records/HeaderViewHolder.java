package com.example.split_lah.ui.records;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;

public class HeaderViewHolder extends RecyclerView.ViewHolder {
    public TextView monthHeader;

    public HeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        monthHeader = itemView.findViewById(R.id.tv_month_header);
    }
}