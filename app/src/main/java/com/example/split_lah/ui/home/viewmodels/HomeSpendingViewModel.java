package com.example.split_lah.ui.home.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class HomeSpendingViewModel extends ViewModel {
    private static final String TAG = "HomeSpendingViewModel";
    private String currentGroupId;
    private String currentUserId;
    private final FirebaseFirestore db;

    // LiveData for UI updates
    private final MutableLiveData<String> totalSpending = new MutableLiveData<>("0.00");
    private final MutableLiveData<String> dateRangeText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> currencySymbol = new MutableLiveData<>("$");

    // Listener for real-time updates
    private ListenerRegistration transactionsListener;

    public HomeSpendingViewModel() {
        db = FirebaseFirestore.getInstance();
        updateDateRange(); // Initialize date range text
    }

    // Getters for LiveData
    public LiveData<String> getTotalSpending() {
        return totalSpending;
    }

    public LiveData<String> getDateRangeText() {
        return dateRangeText;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrentGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Log.w(TAG, "Ignoring empty group ID");
            totalSpending.setValue("0.00");
            return;
        }

        boolean groupChanged = !groupId.equals(currentGroupId);
        this.currentGroupId = groupId;

        if (groupChanged) {
            // Reset state when group changes
            totalSpending.setValue("0.00");

            // Remove previous listener if exists
            if (transactionsListener != null) {
                transactionsListener.remove();
                transactionsListener = null;
            }
        }

        // Only fetch if we have both group ID and user ID
        if (currentUserId != null && !currentUserId.isEmpty()) {
            fetchUserSpending();
        }
    }

    public void setCurrentUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Ignoring empty user ID");
            return;
        }

        boolean userChanged = !userId.equals(currentUserId);
        this.currentUserId = userId;

        if (userChanged && currentGroupId != null && !currentGroupId.isEmpty()) {
            fetchUserSpending();
        }
    }

    private void fetchUserSpending() {
        if (currentGroupId == null || currentUserId == null) {
            Log.w(TAG, "Cannot fetch spending without group ID and user ID");
            return;
        }

        isLoading.setValue(true);

        // Remove previous listener if exists
        if (transactionsListener != null) {
            transactionsListener.remove();
        }

        // Create a listener for real-time updates
        transactionsListener = db.collection("permanent_grp")
                .document(currentGroupId)
                .collection("transactions")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        isLoading.setValue(false);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        totalSpending.setValue("0.00");
                        isLoading.setValue(false);
                        // Use current week if no transactions
                        updateDefaultDateRange();
                        return;
                    }

                    double totalAmount = 0.0;
                    String lastCurrency = "$"; // Default currency if none found

                    // Track transaction dates
                    Date earliestDate = null;
                    Date latestDate = null;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> splits = (Map<String, Object>) doc.get("splits");

                        // Get transaction date
                        Timestamp createdAt = doc.getTimestamp("created_at");
                        if (createdAt != null) {
                            Date transactionDate = createdAt.toDate();

                            // Update earliest and latest dates
                            if (earliestDate == null || transactionDate.before(earliestDate)) {
                                earliestDate = transactionDate;
                            }

                            if (latestDate == null || transactionDate.after(latestDate)) {
                                latestDate = transactionDate;
                            }
                        }

                        if (splits != null && splits.containsKey(currentUserId)) {
                            // User exists in this transaction's splits
                            Object userSplitObj = splits.get(currentUserId);

                            if (userSplitObj != null) {
                                try {
                                    // Try to parse as a number (could be String, Double, Long, etc.)
                                    double userAmount;
                                    if (userSplitObj instanceof String) {
                                        userAmount = Double.parseDouble((String) userSplitObj);
                                    } else if (userSplitObj instanceof Number) {
                                        userAmount = ((Number) userSplitObj).doubleValue();
                                    } else {
                                        Log.w(TAG, "Unknown type for split amount: " + userSplitObj.getClass().getName());
                                        continue;
                                    }

                                    totalAmount += userAmount;

                                    // Get currency if available
                                    String docCurrency = doc.getString("currency_code");
                                    if (docCurrency != null && !docCurrency.isEmpty()) {
                                        lastCurrency = docCurrency;
                                    }

                                    Log.d(TAG, "Found user split: " + userAmount + " " + lastCurrency +
                                            " for transaction " + doc.getId());
                                } catch (NumberFormatException ex) {
                                    Log.e(TAG, "Error parsing amount: " + userSplitObj, ex);
                                }
                            }
                        }
                    }

                    // Format and update UI
                    String formattedAmount = String.format(Locale.getDefault(), "%.2f", totalAmount);
                    Log.d(TAG, "Total spending calculated: " + formattedAmount + " " + lastCurrency);

                    totalSpending.postValue(formattedAmount);
                    currencySymbol.postValue(lastCurrency);

                    // Update date range with actual transaction dates
                    updateActualDateRange(earliestDate, latestDate);

                    isLoading.postValue(false);
                });
    }


    // Update date range based on actual transaction dates
    private void updateActualDateRange(Date earliestDate, Date latestDate) {
        if (earliestDate == null || latestDate == null) {
            updateDefaultDateRange();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        // If earliest and latest are in the same year
        if (isSameYear(earliestDate, latestDate)) {
            SimpleDateFormat sdfNoYear = new SimpleDateFormat("dd MMM", Locale.getDefault());

            // If earliest and latest are the same day
            if (isSameDay(earliestDate, latestDate)) {
                String dateText = sdf.format(earliestDate);
                dateRangeText.setValue(dateText);
            } else {
                // Show range with year only once
                String startText = sdfNoYear.format(earliestDate);
                String endText = sdf.format(latestDate);
                dateRangeText.setValue(startText + " - " + endText);
            }
        } else {
            // Different years, show full dates
            String dateText = sdf.format(earliestDate) + " - " + sdf.format(latestDate);
            dateRangeText.setValue(dateText);
        }

        Log.d(TAG, "Updated date range: " + dateRangeText.getValue());
    }

    // Helper to check if two dates are in the same year
    private boolean isSameYear(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    // Helper to check if two dates are on the same day
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    // Rename the old method to be more descriptive
    private void updateDefaultDateRange() {
        // Get current week date range (for display purposes)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date startDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date endDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        String dateText = sdf.format(startDate) + " - " + sdf.format(endDate);
        dateRangeText.setValue(dateText);
    }

    private void updateDateRange() {
        // Get current week date range (for display purposes)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date startDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date endDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        String dateText = sdf.format(startDate) + " - " + sdf.format(endDate);
        dateRangeText.setValue(dateText);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up the listener when ViewModel is destroyed
        if (transactionsListener != null) {
            transactionsListener.remove();
        }
    }
}