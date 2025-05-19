package com.example.split_lah.ui.home.components;

import androidx.navigation.NavController;

import com.example.split_lah.R;
import com.example.split_lah.databinding.FragmentHomeBinding;

/**
 * Manages navigation from the home fragment
 */
public class NavigationManager {
    private final FragmentHomeBinding binding;
    private final NavController navController;

    public NavigationManager(FragmentHomeBinding binding, NavController navController) {
        this.binding = binding;
        this.navController = navController;
    }

    public void setupNavigationButtons() {
        // Set up click listener for Recent Split See All
        binding.btnHomeRecentSplitSeeAll.setOnClickListener(v -> {
            navController.navigate(R.id.nav_records);
        });

        // Set up click listener for Debt Relation See All
        binding.btnHomeDebtRelationsSeeAll.setOnClickListener(v -> {
            navController.navigate(R.id.nav_debt_relation);
        });

        // Set up click listener for Net Balances See All
        binding.btnHomeNetBalancesSeeAll.setOnClickListener(v -> {
            navController.navigate(R.id.nav_net_balances);
        });

        binding.homeMembersBar.setOnClickListener(v -> {
            navController.navigate(R.id.nav_members);
        });
    }
}