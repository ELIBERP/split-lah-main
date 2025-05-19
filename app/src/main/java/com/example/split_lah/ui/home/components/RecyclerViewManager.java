package com.example.split_lah.ui.home.components;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Generic class to manage RecyclerView setup and empty state handling
 */
public class RecyclerViewManager<T extends RecyclerView.Adapter<?>> {
    private final RecyclerView recyclerView;
    private final View emptyStateView;
    private final Context context;
    private T adapter;

    public RecyclerViewManager(Context context, RecyclerView recyclerView, View emptyStateView) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.emptyStateView = emptyStateView;

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setItemPrefetchEnabled(true);
        layoutManager.setInitialPrefetchItemCount(3);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        // Initial state - hide both recycler view and empty state
        recyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);
    }

    public void setAdapter(T adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);

        // Setup empty state observer
        setupEmptyStateObserver();
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    private void setupEmptyStateObserver() {
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

    public View getEmptyStateView() {
        return emptyStateView;
    }
}