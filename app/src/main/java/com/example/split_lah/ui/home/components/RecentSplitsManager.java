package com.example.split_lah.ui.home.components;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.records.Item;
import com.example.split_lah.ui.records.RecordsAdapter;
import com.example.split_lah.ui.records.RecordsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Specialized manager for Recent Splits section
 */
public class RecentSplitsManager {
    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final RecyclerViewManager<RecordsAdapter> recyclerViewManager;
    private final RecordsViewModel recordsViewModel;
    private final SharedViewModel sharedViewModel;
    private final NavController navController;

    public RecentSplitsManager(
            Context context,
            LifecycleOwner lifecycleOwner,
            RecyclerView recyclerView,
            View emptyStateView,
            RecordsViewModel recordsViewModel,
            SharedViewModel sharedViewModel,
            NavController navController) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.recyclerViewManager = new RecyclerViewManager<>(context, recyclerView, emptyStateView);
        this.recordsViewModel = recordsViewModel;
        this.sharedViewModel = sharedViewModel;
        this.navController = navController;
    }

    public void setup() {
        recordsViewModel.getItems().observe(lifecycleOwner, allItems -> {
            if (allItems == null || allItems.isEmpty()) {
                // Directly set visibility since there's no adapter yet
                recyclerViewManager.getRecyclerView().setVisibility(View.GONE);
                recyclerViewManager.getEmptyStateView().setVisibility(View.VISIBLE);
                return;
            }

            // Create a copy of the list so we can sort it without affecting the original
            List<Item> sortedItems = new ArrayList<>(allItems);

            // Sort by date (newest first)
            Collections.sort(sortedItems, (item1, item2) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                    Date date1 = sdf.parse(item1.getDate());
                    Date date2 = sdf.parse(item2.getDate());
                    // Sort in descending order (newest first)
                    return date2.compareTo(date1);
                } catch (Exception e) {
                    return 0;
                }
            });

            // Take only the first two items (most recent)
            List<Item> recentItems = new ArrayList<>();
            int count = 0;
            for (Item item : sortedItems) {
                recentItems.add(item);
                count++;
                if (count >= 2) break;
            }

            if (!recentItems.isEmpty()) {
                // Create adapter with direct transaction items, don't group by month
                RecordsAdapter adapter = new RecordsAdapter(
                        context,
                        recentItems,
                        false  // Don't group by month (no headers)
                );

                // Set click listener on adapter for navigation
                adapter.setOnItemClickListener(transactionId -> {
                    // Navigate to transaction detail fragment
                    Bundle args = new Bundle();
                    args.putString("transactionId", transactionId);
                    args.putString("groupId", sharedViewModel.getGroupId().getValue());
                    navController.navigate(R.id.action_navigation_home_to_transaction_detail, args);
                });

                recyclerViewManager.setAdapter(adapter);
            } else {
                // In case filtering resulted in no items
                recyclerViewManager.getRecyclerView().setVisibility(View.GONE);
                recyclerViewManager.getEmptyStateView().setVisibility(View.VISIBLE);
            }
        });
    }
}