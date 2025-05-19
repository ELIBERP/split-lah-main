package com.example.split_lah.ui.records;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.imageview.ShapeableImageView;

public class ParticipantAdapter extends
        ListAdapter<TransactionDetailViewModel.ParticipantItem, ParticipantAdapter.ViewHolder> {

    protected ParticipantAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<TransactionDetailViewModel.ParticipantItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TransactionDetailViewModel.ParticipantItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull TransactionDetailViewModel.ParticipantItem oldItem,
                        @NonNull TransactionDetailViewModel.ParticipantItem newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull TransactionDetailViewModel.ParticipantItem oldItem,
                        @NonNull TransactionDetailViewModel.ParticipantItem newItem) {
                    return oldItem.getAmount().equals(newItem.getAmount()) &&
                            oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getStatus().equals(newItem.getStatus()) &&
                            oldItem.isOwing() == newItem.isOwing();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_split, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionDetailViewModel.ParticipantItem item = getItem(position);
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
            iconView = itemView.findViewById(R.id.img_participant_icon);
            nameView = itemView.findViewById(R.id.tv_participant_name);
            statusView = itemView.findViewById(R.id.tv_participant_status);
            currencyView = itemView.findViewById(R.id.tv_participant_currency);
            amountView = itemView.findViewById(R.id.tv_participant_amount);
        }

        public void bind(TransactionDetailViewModel.ParticipantItem item) {
            iconView.setImageResource(item.getIconResource());
            nameView.setText(item.getName());
            statusView.setText(item.getStatus());
            currencyView.setText(item.getCurrencyCode());
            amountView.setText(item.getAmount());

            // Set text color based on owing status
            int textColor = item.isOwing() ?
                    R.color.dark_blue : R.color.gray_600;
            statusView.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
        }
    }
}