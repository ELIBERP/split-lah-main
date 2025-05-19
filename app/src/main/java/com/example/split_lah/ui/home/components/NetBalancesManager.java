package com.example.split_lah.ui.home.components;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.net_balances.Item;
import com.example.split_lah.ui.net_balances.NetBalancesAdapter;
import com.example.split_lah.ui.net_balances.NetBalancesViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the net balances section in the home fragment
 */
public class NetBalancesManager {
    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final RecyclerView recyclerView;
    private final View emptyStateView;
    private final NetBalancesViewModel viewModel;
    private final SharedViewModel sharedViewModel;

    public NetBalancesManager(
            Context context,
            LifecycleOwner lifecycleOwner,
            RecyclerView recyclerView,
            View emptyStateView,
            NetBalancesViewModel viewModel,
            SharedViewModel sharedViewModel) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.recyclerView = recyclerView;
        this.emptyStateView = emptyStateView;
        this.viewModel = viewModel;
        this.sharedViewModel = sharedViewModel;
    }

    public void setup() {
        setupRecyclerView();
        observeNetBalances();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(3);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
    }

    private void observeNetBalances() {
        // Get the current user's first name to use for filtering
        sharedViewModel.getUserFirstName().observe(lifecycleOwner, firstName -> {
            if (firstName == null || firstName.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.VISIBLE);
                return;
            }

            // Set up observer that will filter items based on the user's name
            viewModel.getItems().observe(lifecycleOwner, items -> {
                // Filter the items to only show the current user's balance
                List<Item> filteredItems = new ArrayList<>();

                for (Item item : items) {
                    // Match exactly the current user's name
                    if (item.getMember().equals(firstName)) {
                        filteredItems.add(item);
                        // Don't break - there might be multiple entries with different currencies
                    }
                }

                // Use filtered items for the adapter
                NetBalancesAdapter netBalancesAdapter = new NetBalancesAdapter(context, filteredItems);
                recyclerView.setAdapter(netBalancesAdapter);

                // Handle empty state
                updateEmptyState(filteredItems.isEmpty());
            });
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }
}