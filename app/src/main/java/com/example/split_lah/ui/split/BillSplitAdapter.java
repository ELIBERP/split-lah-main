package com.example.split_lah.ui.split;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;

import java.util.List;

public class BillSplitAdapter extends RecyclerView.Adapter<BillSplitViewHolder> {
    private List<String> memberNames;
    private List<String> amounts;
    private List<String> memberIds;
    private String payerId;
    private String currency;
    private List<String> memberIcons;
    private String payerIcon;
    private String paidBy;
    private Context context;

    public BillSplitAdapter(List<String> memberNames, List<String> amounts,
                            List<String> memberIds, String payerId, String currency,
                            List<String> memberIcons, String payerIcon, String paidBy) {
        this.memberNames = memberNames;
        this.amounts = amounts;
        this.memberIds = memberIds;
        this.payerId = payerId;
        this.currency = currency;
        this.memberIcons = memberIcons;
        this.payerIcon = payerIcon;
        this.paidBy = paidBy;
    }

    @Override
    public BillSplitViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.payment_item, parent, false);
        return new BillSplitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BillSplitViewHolder holder, int position) {
        String debtorName = memberNames.get(position);
        String amount = amounts.get(position);
        String debtorIcon = memberIcons.get(position);

        try {
            int debtorIconResId = context.getResources().getIdentifier(debtorIcon, "drawable", context.getPackageName());
            int payerIconResId = context.getResources().getIdentifier(payerIcon, "drawable", context.getPackageName());

            holder.bind(debtorName, paidBy, amount, debtorIconResId, payerIconResId, currency);
        } catch (Exception e) {
            holder.bind(debtorName, paidBy, amount, R.drawable.giraffe, R.drawable.giraffe, currency);
        }
    }

    @Override
    public int getItemCount() {
        return memberNames.size();
    }
}
