package com.example.split_lah.ui.net_balances;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.databinding.FragmentNetBalancesBinding;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.debt_relation.DebtRelationViewModel;

import java.util.List;

public class NetBalancesFragment extends Fragment {

    private FragmentNetBalancesBinding binding;
    private NetBalancesViewModel netBalancesViewModel;
    private SharedViewModel sharedViewModel;
    private TextView groupNameTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Use activity scope instead of fragment scope
        netBalancesViewModel = new ViewModelProvider(requireActivity()).get(NetBalancesViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentNetBalancesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        String currentGroupName = sharedViewModel.getGroupName().getValue();
        groupNameTextView = binding.tvGroupNameNetBalances;

        if (currentGroupName != null && !currentGroupName.isEmpty()) {
            groupNameTextView.setText(currentGroupName);
        } else {
            Log.w("NetBalancesFragment", "No group name available - data won't load");
        }

        // Connect to DebtRelationViewModel (activity-scoped)
        DebtRelationViewModel debtRelationViewModel =
                new ViewModelProvider(requireActivity()).get(DebtRelationViewModel.class);
        netBalancesViewModel.setDebtRelationViewModel(debtRelationViewModel);

        // Also get the SharedViewModel to observe group changes
        SharedViewModel sharedViewModel =
                new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupRecyclerView();

        // Get the current group ID first (important to do this AFTER setupRecyclerView)
        String currentGroupId = sharedViewModel.getGroupId().getValue();

        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            // Set group ID immediately if available
            netBalancesViewModel.setCurrentGroupId(currentGroupId);
            debtRelationViewModel.setCurrentGroupId(currentGroupId);
        } else {
            // If no group is selected, hide loading indicator
            binding.loadingIndicator.setVisibility(View.GONE);
        }

        // Then set up the observer for future group changes
        sharedViewModel.getGroupId().observe(getViewLifecycleOwner(), groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                // Add this line to set the group ID directly on NetBalancesViewModel
                netBalancesViewModel.setCurrentGroupId(groupId);

                // This will trigger the DebtRelationViewModel to update
                debtRelationViewModel.setCurrentGroupId(groupId);
            } else {
                // No group selected, ensure loading is stopped
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });

        return root;
    }

    private void checkEmptyState(List<Item> items, View emptyStateView) {
        RecyclerView recyclerView = binding.recyclerViewNetBalances;
        if (items == null || items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupRecyclerView() {
        RecyclerView netBalancesRecyclerView = binding.recyclerViewNetBalances;
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setItemPrefetchEnabled(false);
        layoutManager.setInitialPrefetchItemCount(2);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        netBalancesRecyclerView.setLayoutManager(layoutManager);

        // Initial state
        netBalancesRecyclerView.setVisibility(View.GONE);
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        // Add timeout handler to prevent infinite loading
        new Handler().postDelayed(() -> {
            // If we're still showing the loading indicator after 5 seconds,
            // assume something went wrong and show empty state
            if (binding != null && binding.loadingIndicator.getVisibility() == View.VISIBLE) {
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        }, 5000); // 5 second timeout

        // Observe loading state
        netBalancesViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null) {
                binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        netBalancesViewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (binding == null) return; // Fragment view might be destroyed

            NetBalancesAdapter adapter = new NetBalancesAdapter(requireContext(), items);
            netBalancesRecyclerView.setAdapter(adapter);

            // Check empty state
            if (items == null || items.isEmpty()) {
                netBalancesRecyclerView.setVisibility(View.GONE);
            } else {
                netBalancesRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }
}
