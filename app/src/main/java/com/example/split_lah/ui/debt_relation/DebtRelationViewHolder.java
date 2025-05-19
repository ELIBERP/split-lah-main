package com.example.split_lah.ui.debt_relation;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class DebtRelationViewHolder extends RecyclerView.ViewHolder {
    public ShapeableImageView iconPayer;
    public ShapeableImageView iconPayee;
    public TextView payer;
    public TextView payee;
    public TextView currency;
    public TextView amount;
    public Button settleDebtButton;

    public DebtRelationViewHolder(@NonNull View itemView, boolean hasSettleButton) {
        super(itemView);

        // Try to find views with home_ prefix first, fall back to regular IDs
        iconPayer = (ShapeableImageView) findViewByIdOrAlternative(itemView,
                R.id.img_home_debt_relation_payer, R.id.img_debt_relation_payer);
        iconPayee = (ShapeableImageView) findViewByIdOrAlternative(itemView,
                R.id.img_home_debt_relation_payee, R.id.img_debt_relation_payee);
        payer = (TextView) findViewByIdOrAlternative(itemView,
                R.id.tv_home_debt_relation_payer, R.id.tv_debt_relation_payer);
        payee = (TextView) findViewByIdOrAlternative(itemView,
                R.id.tv_home_debt_relation_payee, R.id.tv_debt_relation_payee);
        currency = (TextView) findViewByIdOrAlternative(itemView,
                R.id.tv_home_debt_relation_currency, R.id.tv_debt_relation_currency);
        amount = (TextView) findViewByIdOrAlternative(itemView,
                R.id.tv_home_debt_relation_amount, R.id.tv_debt_relation_amount);

        if (hasSettleButton) {
            settleDebtButton = itemView.findViewById(R.id.btn_settle_debt);
            if (settleDebtButton != null) {
                settleDebtButton.setOnClickListener(v -> {
                    // Handle settle debt button click
                    Toast.makeText(itemView.getContext(), "Payer: " + payer.getText(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
    private View findViewByIdOrAlternative(View parent, int primaryId, int alternativeId) {
        View view = parent.findViewById(primaryId);
        return view != null ? view : parent.findViewById(alternativeId);
    }
}

