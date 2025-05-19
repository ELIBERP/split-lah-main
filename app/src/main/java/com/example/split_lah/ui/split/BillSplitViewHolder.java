package com.example.split_lah.ui.split;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class BillSplitViewHolder extends RecyclerView.ViewHolder {
    private ShapeableImageView payerImageView;
    private ShapeableImageView payeeImageView;
    private TextView payerTextView;
    private TextView payeeTextView;
    private TextView statusTextView;
    private TextView amountTextView;

    public BillSplitViewHolder(View itemView) {
        super(itemView);
        payerImageView = itemView.findViewById(R.id.img_home_debt_relation_payer);
        payeeImageView = itemView.findViewById(R.id.img_home_debt_relation_payee);
        payerTextView = itemView.findViewById(R.id.tv_home_debt_relation_payer);
        payeeTextView = itemView.findViewById(R.id.tv_home_debt_relation_payee);
        statusTextView = itemView.findViewById(R.id.tv_needs_to_pay);
        amountTextView = itemView.findViewById(R.id.tv_home_debt_relation_amount);
    }

    public void bind(String debtorName, String payerName, String amount,
                     int debtorIconResId, int payerIconResId, String currency) {
        // Left side - payee
        payerTextView.setText(debtorName);
        payerImageView.setImageResource(debtorIconResId);

        // Right side - payer
        payeeTextView.setText(payerName);
        payeeImageView.setImageResource(payerIconResId);

        statusTextView.setText("needs to pay");

        // Amount rounded to 2dp
        try {
            double amountValue = Double.parseDouble(amount);
            String formattedAmount = String.format("%.2f", amountValue);
            amountTextView.setText(currency + " " + formattedAmount);
        } catch (NumberFormatException e) {
            amountTextView.setText(currency + " " + amount);
        }
    }
}
