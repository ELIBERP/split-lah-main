package com.example.split_lah.ui.home.components;

import androidx.lifecycle.LifecycleOwner;

import com.example.split_lah.R;
import com.example.split_lah.databinding.FragmentHomeBinding;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.home.viewmodels.HomeSpendingViewModel;

/**
 * Manages user information display in home fragment
 */
public class UserInfoManager {
    private final FragmentHomeBinding binding;
    private final LifecycleOwner lifecycleOwner;
    private final SharedViewModel sharedViewModel;
    private final HomeSpendingViewModel homeSpendingViewModel;

    public UserInfoManager(
            FragmentHomeBinding binding,
            LifecycleOwner lifecycleOwner,
            SharedViewModel sharedViewModel,
            HomeSpendingViewModel homeSpendingViewModel) {
        this.binding = binding;
        this.lifecycleOwner = lifecycleOwner;
        this.sharedViewModel = sharedViewModel;
        this.homeSpendingViewModel = homeSpendingViewModel;
    }

    public void setup() {
        setupUserInfoObservers();
        setupSpendingObservers();
        setupGroupObservers();
    }

    private void setupUserInfoObservers() {
        // Observe user's first name for UI
        sharedViewModel.getUserFirstName().observe(lifecycleOwner, firstName -> {
            if (firstName != null && !firstName.isEmpty()) {
                binding.tvHomeUserName.setText(firstName);
            } else {
                binding.tvHomeUserName.setText("User");
            }
        });

        // Observe user's icon for UI
        sharedViewModel.getUserIcon().observe(lifecycleOwner, iconName -> {
            if (iconName != null && !iconName.isEmpty()) {
                binding.imgHomeUser.setImageResource(IconUtils.getIconResourceId(iconName));
            } else {
                binding.imgHomeUser.setImageResource(R.drawable.ic_user_placeholder);
            }
        });
    }

    private void setupSpendingObservers() {
        // Observe spending data
        homeSpendingViewModel.getTotalSpending().observe(lifecycleOwner, amount -> {
            String currency = homeSpendingViewModel.getCurrencySymbol().getValue();
            binding.tvHomeMySpendingAmount.setText(currency + amount);
        });

        // Observe date range
        homeSpendingViewModel.getDateRangeText().observe(lifecycleOwner, dateRange -> {
            binding.tvMySpendingDate.setText(dateRange);
        });
    }

    private void setupGroupObservers() {
        // Observe group name for UI
        sharedViewModel.getGroupName().observe(lifecycleOwner, groupName -> {
            if (groupName != null && !groupName.isEmpty()) {
                binding.textGroupName.setText(groupName);
            } else {
                binding.textGroupName.setText("No group selected");
            }
        });
    }
}