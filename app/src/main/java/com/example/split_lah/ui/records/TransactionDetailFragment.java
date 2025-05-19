package com.example.split_lah.ui.records;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.split_lah.R;
import com.example.split_lah.databinding.FragmentDetailedRecordsBinding;

public class TransactionDetailFragment extends Fragment {
    private static final String TAG = "TransactionDetailFrag";

    private FragmentDetailedRecordsBinding binding;
    private TransactionDetailViewModel viewModel;
    private PayerAdapter payerAdapter;
    private ParticipantAdapter participantAdapter;
    private Toolbar toolbar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailedRecordsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionDetailViewModel.class);

        setupToolbar();

        // Get transaction ID from arguments
        if (getArguments() == null ||
                getArguments().getString("transactionId") == null ||
                getArguments().getString("groupId") == null) {

            Log.e(TAG, "Missing required arguments: transactionId or groupId");
            Navigation.findNavController(view).navigateUp();
            return;
        }

        String transactionId = getArguments().getString("transactionId");
        String groupId = getArguments().getString("groupId");

        setupRecyclerViews();

        // Load transaction data
        viewModel.loadTransactionData(groupId, transactionId);

        // Observe the data
        observeViewModel();
    }

    private void setupToolbar() {
        // Get the main toolbar from the activity
        toolbar = requireActivity().findViewById(R.id.toolbar);

        if (toolbar != null) {
            // Save the current state
            toolbar.setTag(R.id.toolbar, toolbar.getNavigationIcon());

            // Set the toolbar title and back button
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle("Transaction Details");
            }

            // Set navigation icon and click listener
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> {
                Navigation.findNavController(requireView()).navigateUp();
            });
        }
    }

    private void restoreToolbar() {
        if (toolbar != null) {
            // Restore original toolbar configuration
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(R.string.app_name);
            }

            // Restore the original navigation icon
            toolbar.setNavigationIcon(R.drawable.baseline_menu_24);

            // Set drawer toggle listener
            toolbar.setNavigationOnClickListener(v -> {
                // This should trigger the drawer to open
                // You may need to call activity.openDrawer() here if needed
            });
        }
    }

    private void setupRecyclerViews() {
        // Setup payers recycler view
        payerAdapter = new PayerAdapter();
        binding.recyclerPayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPayers.setAdapter(payerAdapter);

        // Setup participants recycler view
        participantAdapter = new ParticipantAdapter();
        binding.recyclerParticipants.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerParticipants.setAdapter(participantAdapter);

        // Delete button setup removed as requested
    }

    private void observeViewModel() {
        // Loading state controls visibility of content and loading indicator
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.contentContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        // Only populate data - it won't show until loading is complete
        viewModel.getTransactionDetail().observe(getViewLifecycleOwner(), detail -> {
            if (detail == null) return;

            binding.tvTransactionTitle.setText(detail.getTitle());
            binding.tvTransactionDate.setText(detail.getDate());
            binding.imgTransactionIcon.setImageResource(detail.getIconResource());
            binding.tvTransactionCurrency.setText(detail.getCurrencyCode());
            binding.tvTransactionAmount.setText(detail.getAmount());
        });

        // Payer list
        viewModel.getPayers().observe(getViewLifecycleOwner(), payers ->
                payerAdapter.submitList(payers));

        // Participant/splits list
        viewModel.getParticipants().observe(getViewLifecycleOwner(), participants ->
                participantAdapter.submitList(participants));
    }

    @Override
    public void onDestroyView() {
        restoreToolbar();
        super.onDestroyView();
        binding = null;
    }
}