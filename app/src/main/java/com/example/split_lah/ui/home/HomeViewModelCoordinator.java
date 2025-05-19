package com.example.split_lah.ui.home;

import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.debt_relation.DebtRelationViewModel;
import com.example.split_lah.ui.home.viewmodels.HomeSpendingViewModel;
import com.example.split_lah.ui.members.MembersViewModel;
import com.example.split_lah.ui.net_balances.NetBalancesViewModel;
import com.example.split_lah.ui.records.RecordsViewModel;

/**
 * Coordinates ViewModel interactions and dependencies
 */
public class HomeViewModelCoordinator {
    private final LifecycleOwner lifecycleOwner;
    private final DebtRelationViewModel debtRelationViewModel;
    private final MembersViewModel membersViewModel;
    private final NetBalancesViewModel netBalancesViewModel;
    private final HomeSpendingViewModel homeSpendingViewModel;
    private final RecordsViewModel recordsViewModel;
    private final SharedViewModel sharedViewModel;
    private final String currentUserId;

    public HomeViewModelCoordinator(
            LifecycleOwner lifecycleOwner,
            DebtRelationViewModel debtRelationViewModel,
            MembersViewModel membersViewModel,
            NetBalancesViewModel netBalancesViewModel,
            HomeSpendingViewModel homeSpendingViewModel,
            RecordsViewModel recordsViewModel,
            SharedViewModel sharedViewModel,
            String currentUserId) {
        this.lifecycleOwner = lifecycleOwner;
        this.debtRelationViewModel = debtRelationViewModel;
        this.membersViewModel = membersViewModel;
        this.netBalancesViewModel = netBalancesViewModel;
        this.homeSpendingViewModel = homeSpendingViewModel;
        this.recordsViewModel = recordsViewModel;
        this.sharedViewModel = sharedViewModel;
        this.currentUserId = currentUserId;

        // Set up ViewModel dependencies
        netBalancesViewModel.setDebtRelationViewModel(debtRelationViewModel);
    }

    public void setupUserAndGroupObservers() {
        // Update user IDs in ViewModels
        if (currentUserId != null) {
            homeSpendingViewModel.setCurrentUserId(currentUserId);
            membersViewModel.setCurrentUserId(currentUserId);
        }

        // Observe group ID for ViewModels
        sharedViewModel.getGroupId().observe(lifecycleOwner, groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                debtRelationViewModel.setCurrentGroupId(groupId);
                recordsViewModel.setCurrentGroupId(groupId);
                membersViewModel.setCurrentGroupId(groupId);
                netBalancesViewModel.setCurrentGroupId(groupId);
                homeSpendingViewModel.setCurrentGroupId(groupId);
            }
        });
    }

    public void forceRefreshAllData() {
        if (currentUserId == null || sharedViewModel.getGroupId().getValue() == null) {
            return;
        }

        // Force refresh all ViewModels
        String currentGroupId = sharedViewModel.getGroupId().getValue();
        Log.d("HomeViewModelCoordinator", "Force refreshing all data for group: " + currentGroupId);

        // Remove listeners first
        recordsViewModel.invalidateCache();

        // Delay the refresh slightly to avoid race conditions
        new Handler().postDelayed(() -> {
            // Force refresh on all ViewModels
            recordsViewModel.forceRefresh();
            debtRelationViewModel.forceRefresh();
            netBalancesViewModel.forceRefresh();
            membersViewModel.forceRefresh();
            homeSpendingViewModel.setCurrentGroupId(currentGroupId);
        }, 100);
    }
}