package com.example.split_lah.ui.records;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class PayerAdapter extends ListAdapter<TransactionDetailViewModel.PayerItem, PayerAdapter.ViewHolder> {

    protected PayerAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<TransactionDetailViewModel.PayerItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TransactionDetailViewModel.PayerItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull TransactionDetailViewModel.PayerItem oldItem,
                        @NonNull TransactionDetailViewModel.PayerItem newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull TransactionDetailViewModel.PayerItem oldItem,
                        @NonNull TransactionDetailViewModel.PayerItem newItem) {
                    return oldItem.getAmount().equals(newItem.getAmount()) &&
                            oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getStatus().equals(newItem.getStatus());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_payer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionDetailViewModel.PayerItem item = getItem(position);
        holder.bind(item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView iconView;
        private final TextView nameView;
        private final TextView statusView;
        private final TextView currencyView;
        private final TextView amountView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.img_payer_icon);
            nameView = itemView.findViewById(R.id.tv_payer_name);
            statusView = itemView.findViewById(R.id.tv_payer_status);
            currencyView = itemView.findViewById(R.id.tv_payer_currency);
            amountView = itemView.findViewById(R.id.tv_payer_amount);
        }

        public void bind(TransactionDetailViewModel.PayerItem item) {
            iconView.setImageResource(item.getIconResource());
            nameView.setText(item.getName());
            statusView.setText(item.getStatus());
            currencyView.setText(item.getCurrencyCode());
            amountView.setText(item.getAmount());
        }
    }
}