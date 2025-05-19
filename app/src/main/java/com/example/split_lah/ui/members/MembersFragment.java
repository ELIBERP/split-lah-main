package com.example.split_lah.ui.members;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.databinding.FragmentMembersBinding;
import com.example.split_lah.shared_view_model.SharedViewModel;

public class MembersFragment extends Fragment {
    private FragmentMembersBinding binding;
    private MembersViewModel membersViewModel;
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMembersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        membersViewModel = new ViewModelProvider(this).get(MembersViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);


        setupRecyclerView();

        // Get current group ID
        String currentGroupId = sharedViewModel.getGroupId().getValue();
        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            membersViewModel.setCurrentGroupId(currentGroupId);
        } else {
            Log.w("MembersFragment", "No group ID available");
        }

        // Observe group ID changes
        sharedViewModel.getGroupId().observe(getViewLifecycleOwner(), groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                membersViewModel.setCurrentGroupId(groupId);
            }
        });

        return root;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewMembersList;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initially hide the recyclerView and show loading
        recyclerView.setVisibility(View.GONE);
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        membersViewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            // Hide loading indicator
            binding.loadingIndicator.setVisibility(View.GONE);

            if (items == null || items.isEmpty()) {
                recyclerView.setVisibility(View.GONE);

            } else {
                recyclerView.setVisibility(View.VISIBLE);


                MembersAdapter adapter = new MembersAdapter(requireContext(), items);
                recyclerView.setAdapter(adapter);

                Log.d("MembersFragment", "Displaying " + items.size() + " members");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}