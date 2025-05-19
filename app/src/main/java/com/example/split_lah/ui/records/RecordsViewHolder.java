package com.example.split_lah.ui.records;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class RecordsViewHolder extends RecyclerView.ViewHolder {
    public ImageView icon;
    public ShapeableImageView member1;
    public ShapeableImageView member2;
    public ShapeableImageView member3;
    public ShapeableImageView member4;
    public ShapeableImageView overlayImage;
    public TextView title;
    public TextView date;
    public TextView amount;
    public TextView currency;
    public TextView payer;
    public TextView overlayText;

    public RecordsViewHolder(@NonNull View itemView) {
        super(itemView);
        icon = itemView.findViewById(R.id.img_records_card_icon);
        title = itemView.findViewById(R.id.tv_records_card_title);
        date = itemView.findViewById(R.id.tv_records_card_date);
        amount = itemView.findViewById(R.id.tv_records_card_amount);
        payer = itemView.findViewById(R.id.tv_records_card_payer);
        currency = itemView.findViewById(R.id.tv_records_card_currency);
        member1 = itemView.findViewById(R.id.img_records_card_member1);
        member2 = itemView.findViewById(R.id.img_records_card_member2);
        member3 = itemView.findViewById(R.id.img_records_card_member3);
        member4 = itemView.findViewById(R.id.img_records_card_member4);
        overlayImage = itemView.findViewById(R.id.img_records_card_overlay_image);
        overlayText = itemView.findViewById(R.id.tv_records_card_overlay_text);
    }
}