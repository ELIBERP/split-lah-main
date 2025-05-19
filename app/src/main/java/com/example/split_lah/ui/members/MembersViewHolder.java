package com.example.split_lah.ui.members;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class MembersViewHolder extends RecyclerView.ViewHolder {
    public TextView memberName;
    public TextView memberRole;
    public ShapeableImageView memberIcon;

    public MembersViewHolder(@NonNull View itemView) {
        super(itemView);
        memberName = itemView.findViewById(R.id.tv_members_card_name);
        memberRole = itemView.findViewById(R.id.tv_members_card_role);
        memberIcon = itemView.findViewById(R.id.img_members_card_icon);
    }
}
