package com.example.split_lah.ui.home.components;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.debt_relation.DebtRelationAdapter;
import com.example.split_lah.ui.debt_relation.DebtRelationViewModel;
import com.example.split_lah.ui.debt_relation.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the debt relation section in the home fragment
 */
public class DebtRelationManager {
    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final RecyclerView recyclerView;
    private final View emptyStateView;
    private final DebtRelationViewModel viewModel;
    private final SharedViewModel sharedViewModel;

    public DebtRelationManager(
            Context context,
            LifecycleOwner lifecycleOwner,
            RecyclerView recyclerView,
            View emptyStateView,
            DebtRelationViewModel viewModel,
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
        observeDebtRelations();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(3);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
    }

    private void observeDebtRelations() {
        // Get the current user's first name to use for filtering
        sharedViewModel.getUserFirstName().observe(lifecycleOwner, firstName -> {
            if (firstName == null || firstName.isEmpty()) {
                return; // Can't filter without a name
            }

            // Set up observer that will filter items based on the user's name
            viewModel.getItems().observe(lifecycleOwner, items -> {
                // Filter the items to only show those involving the current user
                List<DebtRelationViewModel.Item> filteredItems = new ArrayList<>();

                for (DebtRelationViewModel.Item item : items) {
                    // Check if this user is either the payer or payee
                    if (item.getPayer().contains(firstName) || item.getPayee().contains(firstName)) {
                        filteredItems.add(item);
                    }
                }

                // Use the modified adapter with home layout and filtered items
                DebtRelationAdapter debtRelationAdapter = new DebtRelationAdapter(
                        context,
                        filteredItems,
                        R.layout.home_debt_relation_card,
                        false // No settle button in home layout
                );
                recyclerView.setAdapter(debtRelationAdapter);

                // Handle empty state
                setupEmptyStateObserver(debtRelationAdapter);
            });
        });
    }

    private void setupEmptyStateObserver(RecyclerView.Adapter adapter) {
        // Check initial state
        checkEmptyState(adapter.getItemCount());

        // Register observer for changes
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmptyState(adapter.getItemCount());
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmptyState(adapter.getItemCount());
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmptyState(adapter.getItemCount());
            }
        });
    }

    private void checkEmptyState(int itemCount) {
        if (itemCount == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }
}