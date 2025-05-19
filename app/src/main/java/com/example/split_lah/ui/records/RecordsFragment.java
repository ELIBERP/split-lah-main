package com.example.split_lah.ui.records;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.split_lah.R;
import com.example.split_lah.databinding.FragmentRecordsBinding;
import com.example.split_lah.shared_view_model.SharedViewModel;

import java.util.List;

public class RecordsFragment extends Fragment {

    private FragmentRecordsBinding binding;
    private RecordsViewModel recordsViewModel;
    private SearchView searchView;
    private RecordsAdapter adapter;
    private SharedViewModel sharedViewModel;
    private TextView groupNameTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        recordsViewModel = new ViewModelProvider(requireActivity()).get(RecordsViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentRecordsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        String currentGroupId = sharedViewModel.getGroupId().getValue();
        String currentGroupName = sharedViewModel.getGroupName().getValue();
        groupNameTextView = binding.tvRecordsGroupName;

        if (currentGroupName != null && !currentGroupName.isEmpty()) {
            groupNameTextView.setText(currentGroupName);
        } else {
            Log.w("RecordsFragment", "No group name available - data won't load");
        }

        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            recordsViewModel.setCurrentGroupId(currentGroupId);
        } else {
            Log.w("RecordsFragment", "No group ID available - data won't load");
        }

        // Observe group ID changes
        sharedViewModel.getGroupId().observe(getViewLifecycleOwner(), groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                recordsViewModel.setCurrentGroupId(groupId);
            }
        });

        setupRecyclerView();
        // Initialize the searchView from the layout
        searchView = binding.recordsSearch;

        // Set up search view listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return false;
            }
        });

        return root;
    }

    private void checkEmptyState(List<?> items, View emptyStateView) {
        RecyclerView recyclerView = binding.recyclerViewRecords;
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
        RecyclerView recordsRecyclerView = binding.recyclerViewRecords;
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recordsRecyclerView.setLayoutManager(layoutManager);

        // Initial empty state check with loading indicator
        binding.recyclerViewRecords.setVisibility(View.GONE);
        binding.emptyStateRecords.setVisibility(View.GONE);
        binding.loadingIndicator.setVisibility(View.VISIBLE); // Add a loading indicator to your layout

        recordsViewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            // Hide loading indicator
            binding.loadingIndicator.setVisibility(View.GONE);

            // Check if the items list is empty
            if (items == null || items.isEmpty()) {
                binding.recyclerViewRecords.setVisibility(View.GONE);
                binding.emptyStateRecords.setVisibility(View.VISIBLE);
                return;
            }

            // We have items, show the RecyclerView and update adapter
            binding.recyclerViewRecords.setVisibility(View.VISIBLE);
            binding.emptyStateRecords.setVisibility(View.GONE);


            if (adapter == null) {
                adapter = new RecordsAdapter(requireContext(), items);

                // Set click listener on adapter
                adapter.setOnItemClickListener(transactionId -> {
                    // Navigate to transaction detail fragment
                    Bundle args = new Bundle();
                    args.putString("transactionId", transactionId);
                    args.putString("groupId", sharedViewModel.getGroupId().getValue());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_navigation_records_to_transaction_detail, args);
                });

                recordsRecyclerView.setAdapter(adapter);
            } else {
                adapter.setItems(items);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
