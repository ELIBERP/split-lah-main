package com.example.split_lah.ui.records;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.google.android.material.card.MaterialCardView;

public class RecordsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private final Context context;
    private final List<Item> originalItems;
    private List<ListItem> groupedItems;
    private List<ListItem> filteredItems;
    private OnItemClickListener listener;
    public RecordsAdapter(Context context, List<Item> items) {
        this.context = context;
        this.originalItems = items;
        this.groupedItems = groupByMonth(items);
        this.filteredItems = new ArrayList<>(groupedItems);
    }

    public RecordsAdapter(Context context, List<Item> items, boolean groupByMonth) {
        this.context = context;
        this.originalItems = items;

        if (groupByMonth) {
            this.groupedItems = groupByMonth(items);
        } else {
            // Don't group by month, just use the items directly
            this.groupedItems = new ArrayList<>();
            for (Item item : items) {
                this.groupedItems.add(item); // Add items directly without headers
            }
        }

        this.filteredItems = new ArrayList<>(groupedItems);
    }

    public interface OnItemClickListener {
        void onItemClick(String transactionId);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        this.groupedItems = groupByMonth(items);
        this.filteredItems = new ArrayList<>(groupedItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return filteredItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.layout_header_month, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.records_card, parent, false);
            return new RecordsViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == ListItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            HeaderItem headerItem = (HeaderItem) filteredItems.get(position);
            headerHolder.monthHeader.setText(headerItem.getMonthYear());
        } else {
            RecordsViewHolder recordsHolder = (RecordsViewHolder) holder;
            Item item = (Item) filteredItems.get(position);

            recordsHolder.title.setText(item.getTitle());
            recordsHolder.date.setText(item.getDate());
            recordsHolder.amount.setText(item.getAmount());
            recordsHolder.currency.setText(item.getCurrency());
            recordsHolder.icon.setImageResource(item.getIcon());
            recordsHolder.payer.setText(item.getPayer());

            recordsHolder.member1.setVisibility(View.GONE);
            recordsHolder.member2.setVisibility(View.GONE);
            recordsHolder.member3.setVisibility(View.GONE);
            recordsHolder.member4.setVisibility(View.GONE);
            recordsHolder.overlayImage.setVisibility(View.GONE);
            recordsHolder.overlayText.setVisibility(View.GONE);

            List<Integer> memberIcons = item.getMemberIcons();
            int memberCount = memberIcons.size();

            if (memberIcons.size() > 0) {
                recordsHolder.member1.setVisibility(View.VISIBLE);
                recordsHolder.member1.setImageResource(memberIcons.get(0));
            }
            if (memberIcons.size() > 1) {
                recordsHolder.member2.setVisibility(View.VISIBLE);
                recordsHolder.member2.setImageResource(memberIcons.get(1));
            }
            if (memberIcons.size() > 2) {
                recordsHolder.member3.setVisibility(View.VISIBLE);
                recordsHolder.member3.setImageResource(memberIcons.get(2));
            }
            if (memberIcons.size() > 3) {
                recordsHolder.member4.setVisibility(View.VISIBLE);
                recordsHolder.member4.setImageResource(memberIcons.get(3));
            }
            if (memberIcons.size() > 4) {
                int extraMembers = memberCount - 4;
                recordsHolder.member4.setVisibility(View.VISIBLE);
                recordsHolder.overlayImage.setVisibility(View.VISIBLE);
                recordsHolder.overlayText.setVisibility(View.VISIBLE);
                recordsHolder.overlayText.setText("+" + extraMembers);
            }

            MaterialCardView cardView = (MaterialCardView) ((ViewGroup)holder.itemView).getChildAt(0);

            // Apply the click animation to the MaterialCardView
            cardView.setOnClickListener(v -> {
                // Apply scale animation
                v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            // Return to normal size
                            v.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(100)
                                    .withEndAction(() -> {
                                        // Navigate after animation completes
                                        if (listener != null && item.getId() != null) {
                                            listener.onItemClick(item.getId());
                                        }
                                    })
                                    .start();
                        })
                        .start();
            });
        }
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public List<ListItem> getItems() {
        return filteredItems;
    }

    // Helper method to group items by month
    private List<ListItem> groupByMonth(List<Item> items) {
        List<ListItem> result = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            return result;
        }

        // Sort items by date (newest first)
        Collections.sort(items, (item1, item2) ->
                item2.getDateObject().compareTo(item1.getDateObject()));

        // Group by month-year
        Map<String, List<Item>> groupedMap = new HashMap<>();

        for (Item item : items) {
            String key = item.getMonthYearKey();
            if (!groupedMap.containsKey(key)) {
                groupedMap.put(key, new ArrayList<>());
            }
            groupedMap.get(key).add(item);
        }

        // Sort the keys (months) chronologically - newest first
        List<String> sortedKeys = new ArrayList<>(groupedMap.keySet());
        Collections.sort(sortedKeys, (key1, key2) -> {
            // Extract first item from each group for comparison
            Item item1 = groupedMap.get(key1).get(0);
            Item item2 = groupedMap.get(key2).get(0);
            return item2.getDateObject().compareTo(item1.getDateObject());
        });

        // Add headers and items to result list
        for (String key : sortedKeys) {
            String[] parts = key.split(" ");
            String month = parts[0];
            String year = parts[1];

            // Add header
            result.add(new HeaderItem(month, year));

            // Add items for this month
            result.addAll(groupedMap.get(key));
        }

        return result;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase().trim();
                List<ListItem> filtered = new ArrayList<>();

                if (query.isEmpty()) {
                    filtered = groupedItems;
                } else {
                    // Extract only transaction items matching the query
                    List<Item> matchingTransactions = new ArrayList<>();

                    for (ListItem item : groupedItems) {
                        if (item.getType() == ListItem.TYPE_TRANSACTION) {
                            Item transaction = (Item) item;
                            if (transaction.getTitle().toLowerCase().contains(query)) {
                                matchingTransactions.add(transaction);
                            }
                        }
                    }

                    // Regroup the filtered transactions
                    filtered = groupByMonth(matchingTransactions);
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredItems = (List<ListItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
