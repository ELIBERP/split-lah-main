package com.example.split_lah.ui.net_balances;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.split_lah.models.IconUtils;
import com.example.split_lah.ui.debt_relation.DebtRelationViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetBalancesViewModel extends ViewModel {
    private String currentGroupId;
    private static final String TAG = "NetBalancesViewModel";
    private final MutableLiveData<List<Item>> mItems;
    private DebtRelationViewModel debtRelationViewModel;
    private Observer<List<DebtRelationViewModel.Item>> debtRelationObserver;
    private final FirebaseFirestore db;

    // Map to store user icon details by name
    private final Map<String, String> userIconCache = new HashMap<>();

    // Cache variables
    private static final long CACHE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutes
    private long lastCalculationTimestamp = 0;
    private List<Item> cachedNetBalanceItems = null;
    private boolean isObserverSetup = false;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void setCurrentGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Log.w(TAG, "Ignoring empty group ID");
            mItems.postValue(new ArrayList<>());
            isLoading.postValue(false); // Make sure loading is reset
            return;
        }

        // Set loading to true when changing groups
        isLoading.postValue(true);

        // Track if the group has actually changed
        boolean groupChanged = currentGroupId == null || !groupId.equals(currentGroupId);

        // Always update the current group ID
        String oldGroupId = currentGroupId;
        this.currentGroupId = groupId;

        if (groupChanged) {
            Log.d(TAG, "Group changed from " + oldGroupId + " to: " + groupId + ", clearing all state");

            // Clear ALL state, not just the cache
            invalidateCache();

            // Immediately post an empty list while loading new data
            mItems.postValue(new ArrayList<>());

            // If we have a DebtRelationViewModel, make sure it's updated with the new group
            if (debtRelationViewModel != null) {
                // Force DebtRelationViewModel to refresh too
                debtRelationViewModel.setCurrentGroupId(groupId);
            }
        } else {
            // Check if we can use the cache
            if (isCacheValid()) {
                Log.d(TAG, "Using cached net balance data - " + cachedNetBalanceItems.size() +
                        " items for group: " + currentGroupId);
                mItems.postValue(cachedNetBalanceItems);
                isLoading.postValue(false); // Important: Reset loading when using cache
                return;
            }

            // Even if group didn't change, still need to ensure we have debt data
            forceCalculateFromDebtRelations();
        }
    }

    public NetBalancesViewModel() {
        mItems = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
        // Initial empty list
        mItems.setValue(new ArrayList<>());
    }

    public void setDebtRelationViewModel(DebtRelationViewModel viewModel) {
        // Cleanup previous observer if exists
        if (this.debtRelationViewModel != null && debtRelationObserver != null) {
            this.debtRelationViewModel.getItems().removeObserver(debtRelationObserver);
            isObserverSetup = false;
        }

        this.debtRelationViewModel = viewModel;
        if (viewModel != null) {
            // Create and register the observer
            debtRelationObserver = items -> {
                if (items != null) {
                    Log.d(TAG, "DebtRelation data changed - received " +
                            items.size() + " items for group: " + currentGroupId);

                    // DON'T invalidate cache here - it may cause recursive updates
                    // Only calculate if this is for our current group
                    if (currentGroupId != null && !currentGroupId.isEmpty()) {
                        calculateNetBalances(items);
                    } else {
                        Log.w(TAG, "Received debt relation update but no current group ID - ignoring");
                    }
                } else {
                    Log.d(TAG, "DebtRelation data changed - received null/empty data");
                    mItems.postValue(new ArrayList<>());
                }
            };

            viewModel.getItems().observeForever(debtRelationObserver);
            isObserverSetup = true;

            // Force group ID sync - ensure both ViewModels have the same group ID
            if (currentGroupId != null && !currentGroupId.isEmpty()) {
                viewModel.setCurrentGroupId(currentGroupId);

                // Force immediate update if possible
                if (viewModel.getItems().getValue() != null) {
                    calculateNetBalances(viewModel.getItems().getValue());
                }
            }
        }
    }

    public LiveData<List<Item>> getItems() {
        return mItems;
    }

    // Cache management methods
    private void invalidateCache() {
        Log.d(TAG, "Invalidating net balances cache");
        cachedNetBalanceItems = null;
        lastCalculationTimestamp = 0;
        userIconCache.clear(); // Also clear the user icon cache
    }

    private boolean isCacheValid() {
        if (cachedNetBalanceItems == null) {
            return false;
        }

        // Also check that we're still on the same group
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            return false;
        }

        // Check if cache is still fresh
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastCalculationTimestamp) < CACHE_EXPIRATION_MS;
    }

    private void forceCalculateFromDebtRelations() {
        if (debtRelationViewModel == null) {
            Log.e(TAG, "No DebtRelationViewModel available for calculation");
            isLoading.postValue(false); // Reset loading state when there's no view model
            return;
        }

        // Get current data from DebtRelationViewModel
        List<DebtRelationViewModel.Item> debtItems =
                debtRelationViewModel.getItems().getValue();

        if (debtItems != null && !debtItems.isEmpty()) {
            Log.d(TAG, "Force calculating from " + debtItems.size() + " debt items");
            // BYPASS cache check by calling internal method directly
            calculateNetBalancesInternal(debtItems);
        } else {
            Log.d(TAG, "No debt items available yet, waiting for data");
            // Add this line to handle the case where there's no data yet
            isLoading.postValue(false);
        }
    }

    private void calculateNetBalances(List<DebtRelationViewModel.Item> debtRelations) {
        if (debtRelations == null) {
            Log.d(TAG, "No debt relations provided, posting empty list");
            mItems.postValue(new ArrayList<>());
            isLoading.postValue(false); // Set loading to false
            return;
        }

        // Check if cache is valid
        if (isCacheValid()) {
            Log.d(TAG, "Using cached net balance data - " + cachedNetBalanceItems.size() +
                    " items for group: " + currentGroupId);
            mItems.postValue(cachedNetBalanceItems);
            isLoading.postValue(false); // Set loading to false
            return;
        }

        // Set loading to true before calculation
        isLoading.postValue(true);
        // Cache invalid, perform calculation
        calculateNetBalancesInternal(debtRelations);
    }

    private void calculateNetBalancesInternal(List<DebtRelationViewModel.Item> debtRelations) {
        Log.d(TAG, "Calculating net balances from " + debtRelations.size() +
                " debt relations for group: " + currentGroupId);

        // STEP 1: Get all group members first
        Set<String> allMembers = new HashSet<>();
        Map<String, Integer> memberIcons = new HashMap<>();
        Map<String, String> memberCurrencies = new HashMap<>();

        // Get the complete list of group members from DebtRelationViewModel
        if (debtRelationViewModel != null) {
            List<String> groupMembers = debtRelationViewModel.getGroupMembers();
            if (groupMembers != null) {
                allMembers.addAll(groupMembers);
                Log.d(TAG, "Added " + groupMembers.size() + " members from group roster");
            }
        }

        // Also add any members from debt relations that might not be in the group members list
        for (DebtRelationViewModel.Item debt : debtRelations) {
            // Add payer
            allMembers.add(debt.fromName);
            memberIcons.put(debt.fromName, debt.iconPayer);
            memberCurrencies.put(debt.fromName, debt.currency);

            // Add payee
            allMembers.add(debt.toName);
            memberIcons.put(debt.toName, debt.iconPayee);
            memberCurrencies.put(debt.toName, debt.currency);
        }

        Log.d(TAG, "Total members to process: " + allMembers.size());

        // NEW STEP: Batch fetch user data from Firestore
        fetchUserDataAndContinueCalculation(allMembers, memberIcons, memberCurrencies, debtRelations);
    }

    private void fetchUserDataAndContinueCalculation(
            Set<String> allMembers,
            Map<String, Integer> memberIcons,
            Map<String, String> memberCurrencies,
            List<DebtRelationViewModel.Item> debtRelations) {

        // This map will store userId -> userName mappings
        Map<String, String> userNames = new HashMap<>();
        Map<String, String> userIconNames = new HashMap<>();

        // Count how many users we've processed
        final int[] processedCount = {0};
        final int totalUsers = allMembers.size();

        Log.d(TAG, "Fetching user data for " + totalUsers + " members");

        for (String userId : allMembers) {
            // Check if we already have a cached name
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            // Get the first name from the document
                            String firstName = task.getResult().getString("first_name");
                            String icon = task.getResult().getString("icon");

                            if (firstName != null && !firstName.isEmpty()) {
                                userNames.put(userId, firstName);
                                Log.d(TAG, "Mapped user ID " + userId + " to name: " + firstName);
                            } else {
                                userNames.put(userId,  userId);
                                Log.w(TAG, "User " + userId + " has no first_name field");
                            }

                            if (icon != null && !icon.isEmpty()) {
                                userIconNames.put(userId, icon);
                            }
                        } else {
                            userNames.put(userId, userId);
                            Log.w(TAG, "Could not find user document for ID: " + userId);
                        }

                        // Increment processed count
                        processedCount[0]++;
                        // When all users are processed, continue with calculation
                        if (processedCount[0] >= totalUsers) {
                            continueWithCalculation(
                                    allMembers,
                                    memberIcons,
                                    memberCurrencies,
                                    debtRelations,
                                    userNames,
                                    userIconNames
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user data for ID: " + userId, e);
                        userNames.put(userId, "User " + userId);

                        // Still count as processed even on failure
                        processedCount[0]++;

                        if (processedCount[0] >= totalUsers) {
                            continueWithCalculation(
                                    allMembers,
                                    memberIcons,
                                    memberCurrencies,
                                    debtRelations,
                                    userNames,
                                    userIconNames
                            );
                        }
                    });
        }
    }

    private void continueWithCalculation(
            Set<String> allMembers,
            Map<String, Integer> memberIcons,
            Map<String, String> memberCurrencies,
            List<DebtRelationViewModel.Item> debtRelations,
            Map<String, String> userNames,
            Map<String, String> userIconNames) {

        // STEP 2: Initialize all members with zero balances and neutral "is settled" status
        // Use a map with member name as key (not ID) for easier matching with simplified debts
        Map<String, Item> memberItemMap = new HashMap<>();
        Map<String, String> memberIdToNameMap = new HashMap<>(); // Maps IDs to names for lookups
        List<Item> netBalanceItems = new ArrayList<>();

        for (String memberId : allMembers) {
            // Get member's name from the map we built
            String memberName = userNames.getOrDefault(memberId,  memberId);
            memberIdToNameMap.put(memberId, memberName);

            // Get cached icon or default
            String iconName = null;
            if (userIconNames.containsKey(memberId)) {
                iconName = userIconNames.get(memberId);
            } else if (memberIcons.containsKey(memberId)) {
                iconName = IconUtils.getIconNameFromResourceId(memberIcons.get(memberId));
            }

            if (iconName != null && !userIconCache.containsKey(memberId)) {
                userIconCache.put(memberId, iconName);
            }

            int iconResourceId = IconUtils.getIconResourceId(iconName);
            String currency = memberCurrencies.getOrDefault(memberId, "$");

            // Initialize all members with zero balance
            Item item = new Item(
                    memberName,     // Member name
                    "0.00",         // Zero amount initially
                    currency,       // Currency
                    "is settled",   // Initial status
                    iconResourceId  // Icon
            );

            memberItemMap.put(memberName, item); // Use name as key for easier reference
            netBalanceItems.add(item);
            Log.d(TAG, "Initialized " + memberName + " with zero balance");
        }

        // STEP 3: Calculate running balances for each member based on simplified debts
        Map<String, Double> balances = new HashMap<>();
        for (String memberName : memberItemMap.keySet()) {
            balances.put(memberName, 0.0); // Initialize all balances to zero
        }

        Log.d(TAG, "Processing " + debtRelations.size() + " simplified debt relations");
        // Process all debt relations to update balances
        for (DebtRelationViewModel.Item debt : debtRelations) {
            String fromId = debt.fromName;  // Person who owes money
            String toId = debt.toName;      // Person who is owed money

            // Get names for these IDs
            String fromName = memberIdToNameMap.getOrDefault(fromId, userNames.getOrDefault(fromId, "User " + fromId));
            String toName = memberIdToNameMap.getOrDefault(toId, userNames.getOrDefault(toId, "User " + toId));

            double amount = Double.parseDouble(debt.amount);

            // Update payer's balance (negative adjustment - they owe money)
            double fromBalance = balances.getOrDefault(fromName, 0.0) - amount;
            balances.put(fromName, fromBalance);

            // Update payee's balance (positive adjustment - they are owed money)
            double toBalance = balances.getOrDefault(toName, 0.0) + amount;
            balances.put(toName, toBalance);

            Log.d(TAG, fromName + " owes " + toName + " " + amount +
                    " -> " + fromName + " balance: " + fromBalance +
                    ", " + toName + " balance: " + toBalance);
        }

        // STEP 4: Update the existing items with the calculated balances
        for (String memberName : memberItemMap.keySet()) {
            double balance = balances.getOrDefault(memberName, 0.0);
            Item item = memberItemMap.get(memberName);

            if (item == null) {
                Log.w(TAG, "Missing item for member: " + memberName);
                continue;
            }

            // Format amount (absolute value)
            String formattedAmount = String.format("%.2f", Math.abs(balance));

            // Determine status based on balance
            String owedOrOwing;
            if (Math.abs(balance) < 0.01) {
                owedOrOwing = "is settled";
            } else {
                owedOrOwing = balance < 0 ? "owes" : "is owed";
            }

            // Update item in-place
            item.setAmount(formattedAmount);
            item.setOwedOrOwing(owedOrOwing);

            Log.d(TAG, "Updated " + item.getMember() + ": " + owedOrOwing + " " + formattedAmount);
        }

        // Rest of method (sorting, caching, etc.) remains the same
        List<Item> itemsToSort = new ArrayList<>(memberItemMap.values());

        // Sort items as before
        itemsToSort.sort((item1, item2) -> {
            String status1 = item1.getOwedOrOwing();
            String status2 = item2.getOwedOrOwing();

            if (status1.equals(status2)) {
                // Same status, sort by amount descending
                try {
                    double amount1 = Double.parseDouble(item1.getAmount());
                    double amount2 = Double.parseDouble(item2.getAmount());
                    return Double.compare(amount2, amount1);
                } catch (NumberFormatException e) {
                    return 0;
                }
            } else if ("is owed".equals(status1)) {
                return -1; // "is owed" comes first
            } else if ("is owed".equals(status2)) {
                return 1;  // "is owed" comes first
            } else if ("is settled".equals(status1)) {
                return -1; // "is settled" comes before "owes"
            } else if ("is settled".equals(status2)) {
                return 1;  // "is settled" comes before "owes"
            }
            return 0;
        });
        isLoading.postValue(false);
        // Update cache and post result
        cachedNetBalanceItems = itemsToSort;
        lastCalculationTimestamp = System.currentTimeMillis();

        Log.d(TAG, "Posting " + itemsToSort.size() + " net balance items for group: " + currentGroupId);
        mItems.postValue(itemsToSort);
    }
    public void forceRefresh() {
        isLoading.postValue(true);
        invalidateCache();
        if (debtRelationViewModel != null) {
            // This will trigger recalculation
            debtRelationViewModel.forceRefresh();
        } else {
            isLoading.postValue(false); // Reset loading if no viewmodel available
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Cleanup observer
        if (debtRelationViewModel != null && debtRelationObserver != null) {
            debtRelationViewModel.getItems().removeObserver(debtRelationObserver);
        }
    }
}