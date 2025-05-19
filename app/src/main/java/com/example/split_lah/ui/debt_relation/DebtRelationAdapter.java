package com.example.split_lah.ui.debt_relation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.example.split_lah.shared_view_model.SharedViewModel;

import java.util.ArrayList;
import java.util.List;

public class DebtRelationAdapter extends RecyclerView.Adapter<DebtRelationViewHolder> {
    private static final String TAG = "DebtRelationAdapter";
    private final List<DebtRelationViewModel.Item> items;
    private final Context context;
    private DebtRelationViewModel viewModel;
    private int layoutResId = R.layout.debt_relation_card;
    private boolean showSettleButton = true;
    public DebtRelationAdapter(Context context, List<DebtRelationViewModel.Item> items) {
        this.context = context;
        this.items = new ArrayList<>(items);
        Log.d(TAG, "Created adapter with " + items.size() + " items");
    }

    public DebtRelationAdapter(Context context, List<DebtRelationViewModel.Item> items, DebtRelationViewModel viewModel) {
        this.context = context;
        this.items = new ArrayList<>(items);
        this.viewModel = viewModel;
        Log.d(TAG, "Created adapter with " + items.size() + " items and ViewModel");
    }

    public DebtRelationAdapter(Context context, List<DebtRelationViewModel.Item> items, int layoutResId, boolean showSettleButton) {
        this.context = context;
        this.items = new ArrayList<>(items);
        this.layoutResId = layoutResId;
        this.showSettleButton = showSettleButton;
        Log.d(TAG, "Created adapter with layout: " + layoutResId + ", items: " + items.size() + ", showSettleButton: " + showSettleButton);
    }

    @NonNull
    @Override
    public DebtRelationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutResId, parent, false);
        return new DebtRelationViewHolder(view, showSettleButton);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtRelationViewHolder holder, int position) {
        DebtRelationViewModel.Item item = items.get(position);
        Log.d(TAG, "Binding item: " + item.fromName + " -> " + item.toName);

        // Bind data to views using ViewHolder fields
        if (holder.payer != null) holder.payer.setText(item.fromName);
        if (holder.payee != null) holder.payee.setText(item.toName);
        if (holder.amount != null) holder.amount.setText(item.amount);
        if (holder.currency != null) holder.currency.setText(item.currency);
        if (holder.iconPayer != null) holder.iconPayer.setImageResource(item.iconPayer);
        if (holder.iconPayee != null) holder.iconPayee.setImageResource(item.iconPayee);

        // Set up settle button if it exists and should be shown
        if (holder.settleDebtButton != null) {
            holder.settleDebtButton.setOnClickListener(v -> {
                showSettleDebtBottomSheet(item);
            });
        }
    }

    private void showSettleDebtBottomSheet(DebtRelationViewModel.Item item) {
        if (context instanceof FragmentActivity && viewModel != null) {
            SettleDebtBottomSheetFragment bottomSheet = SettleDebtBottomSheetFragment.newInstance(
                    item.fromName, item.toName, item.amount, item.currency);

            bottomSheet.setOnSettleConfirmedListener((fromName, toName, amount, currency) -> {
                viewModel.settleDebt(fromName, toName, amount, currency);
            });

            bottomSheet.show(((FragmentActivity) context).getSupportFragmentManager(),
                    "SettleDebtBottomSheet");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<DebtRelationViewModel.Item> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }
}