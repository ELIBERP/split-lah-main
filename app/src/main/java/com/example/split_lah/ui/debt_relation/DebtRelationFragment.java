package com.example.split_lah.ui.debt_relation;

import android.os.Bundle;
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

import com.example.split_lah.databinding.FragmentDebtRelationBinding;
import com.example.split_lah.shared_view_model.SharedViewModel;

import java.util.ArrayList;
import java.util.List;

public class DebtRelationFragment extends Fragment {

    private FragmentDebtRelationBinding binding;
    private DebtRelationViewModel debtRelationViewModel;
    private DebtRelationAdapter adapter;
    private SharedViewModel sharedViewModel;
    private TextView groupNameTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDebtRelationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        debtRelationViewModel = new ViewModelProvider(requireActivity()).get(DebtRelationViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        setupRecyclerView();

        String currentGroupName = sharedViewModel.getGroupName().getValue();
        groupNameTextView = binding.tvDebtRelationGroupName;
        if (currentGroupName != null && !currentGroupName.isEmpty()) {
            groupNameTextView.setText(currentGroupName);
        } else {
            Log.w("DebtRelationFragment", "No group name available - data won't load");
        }

        String currentGroupId = sharedViewModel.getGroupId().getValue();
        Log.d("DebtRelationFragment", "Current group ID: " + currentGroupId);
        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            debtRelationViewModel.setCurrentGroupId(currentGroupId);
        } else {
            Log.w("DebtRelationFragment", "No group ID available - data won't load");
        }

        // Observe group ID changes
        sharedViewModel.getGroupId().observe(getViewLifecycleOwner(), groupId -> {
            if (groupId != null && !groupId.isEmpty() &&
                    !groupId.equals(debtRelationViewModel.getCurrentGroupId())) {
                debtRelationViewModel.setCurrentGroupId(groupId);
            }
        });

        return root;
    }

    private void checkEmptyState(List<Item> items, View emptyStateView) {
        RecyclerView recyclerView = binding.recyclerViewDebtRelation;
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
        adapter = null;
    }

    private void setupRecyclerView() {
        RecyclerView debtRelationRecyclerView = binding.recyclerViewDebtRelation;
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        debtRelationRecyclerView.setLayoutManager(layoutManager);

        // Initial UI state
        binding.recyclerViewDebtRelation.setVisibility(View.GONE);
        binding.emptyStateDebtRelation.setVisibility(View.GONE);
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        // Initialize with empty list first
        List<DebtRelationViewModel.Item> initialItems = new ArrayList<>();
        adapter = new DebtRelationAdapter(
                requireContext(),
                initialItems,
                debtRelationViewModel  // FIXED: Pass debtRelationViewModel instead of viewModel
        );
        debtRelationRecyclerView.setAdapter(adapter);

        debtRelationViewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            // Always hide loading indicator when data changes
            binding.loadingIndicator.setVisibility(View.GONE);

            if (items == null || items.isEmpty()) {
                // No data, show empty state
                binding.recyclerViewDebtRelation.setVisibility(View.GONE);
                binding.emptyStateDebtRelation.setVisibility(View.VISIBLE);
                Log.d("DebtRelationFragment", "No debt relations to display");
                return;
            }

            binding.recyclerViewDebtRelation.setVisibility(View.VISIBLE);
            binding.emptyStateDebtRelation.setVisibility(View.GONE);

            // Update adapter items
            adapter.updateItems(items);
            Log.d("DebtRelationFragment", "Displaying " + items.size() + " debt relations");
        });
    }
}