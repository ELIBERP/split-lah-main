package com.example.split_lah.ui.records;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.split_lah.models.IconUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TransactionDetailViewModel extends ViewModel {
    private static final String TAG = "TransDetailViewModel";
    private static final String COLLECTION_GROUPS = "permanent_grp";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";

    // Firebase instance
    private final FirebaseFirestore db;

    // Transaction identifiers
    private String groupId;
    private String transactionId;

    // LiveData properties
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<TransactionDetail> transactionDetail = new MutableLiveData<>();
    private final MutableLiveData<List<PayerItem>> payers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ParticipantItem>> participants = new MutableLiveData<>(new ArrayList<>());

    // Constructor
    public TransactionDetailViewModel() {
        db = FirebaseFirestore.getInstance();
    }

    // Getters for LiveData
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<TransactionDetail> getTransactionDetail() { return transactionDetail; }
    public LiveData<List<PayerItem>> getPayers() { return payers; }
    public LiveData<List<ParticipantItem>> getParticipants() { return participants; }

    // Getters for IDs
    public String getTransactionId() { return transactionId; }
    public String getGroupId() { return groupId; }

    // Load transaction data
    public void loadTransactionData(String groupId, String transactionId) {
        this.groupId = groupId;
        this.transactionId = transactionId;

        isLoading.setValue(true);

        db.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        processTransactionData(document);
                    } else {
                        Log.e(TAG, "Transaction document does not exist");
                        isLoading.setValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transaction data", e);
                    isLoading.setValue(false);
                });
    }

    // Process the transaction document
    private void processTransactionData(DocumentSnapshot document) {
        // Extract transaction data
        String title = document.getString("title");
        String currencyCode = document.getString("currency_code");
        String amount = document.getString("amount");
        Timestamp createdAt = document.getTimestamp("created_at");
        String iconName = document.getString("icon");
        Map<String, Object> splits = (Map<String, Object>) document.get("splits");
        Map<String, Object> paidBy = (Map<String, Object>) document.get("paid_by");

        // Format date
        String date = formatDate(createdAt);

        // Get icon resource
        int iconResource = IconUtils.getIconResourceId(iconName);

        // Set transaction detail
        transactionDetail.setValue(new TransactionDetail(
                title, date, currencyCode, amount, iconResource
        ));

        // Fetch user data for all participants
        if (splits != null || paidBy != null) {
            // Combine unique user IDs
            Set<String> userIds = new HashSet<>();
            if (splits != null) userIds.addAll(splits.keySet());
            if (paidBy != null) userIds.addAll(paidBy.keySet());

            fetchUserData(userIds, userDataMap -> {
                if (paidBy != null) {
                    processPayers(paidBy, userDataMap);
                }

                if (splits != null) {
                    processParticipants(splits, paidBy != null ? paidBy : new HashMap<>(), userDataMap);
                }

                // Only set isLoading to false after ALL data is processed
                isLoading.setValue(false);
            });
        } else {
            // If no splits or paidBy data, we can set isLoading to false here
            isLoading.setValue(false);
        }
    }

    // Fetch user data for all participants
    private void fetchUserData(Set<String> userIds, UserDataCallback callback) {
        if (userIds.isEmpty()) {
            callback.onUserDataFetched(new HashMap<>());
            return;
        }

        Map<String, UserData> userDataMap = new HashMap<>();
        final int[] pendingRequests = {userIds.size()};

        for (String userId : userIds) {
            db.collection(COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        UserData userData;

                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String firstName = task.getResult().getString("first_name");
                            String icon = task.getResult().getString("icon");
                            userData = new UserData(userId, firstName, icon);
                        } else {
                            userData = new UserData(userId, "User " + userId.substring(0, 4), null);
                        }

                        userDataMap.put(userId, userData);

                        // Check if all requests are complete
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            callback.onUserDataFetched(userDataMap);
                        }
                    });
        }
    }

    // Process payers data
    private void processPayers(Map<String, Object> paidBy, Map<String, UserData> userDataMap) {
        List<PayerItem> payerItems = new ArrayList<>();
        String currency = transactionDetail.getValue().getCurrencyCode();

        for (Map.Entry<String, Object> entry : paidBy.entrySet()) {
            String userId = entry.getKey();
            String amount = String.valueOf(entry.getValue());

            UserData userData = userDataMap.get(userId);
            if (userData != null) {
                PayerItem payerItem = new PayerItem(
                        userData.getId(),
                        userData.getName(),
                        "Paid", // Status text
                        amount,
                        currency,
                        IconUtils.getIconResourceId(userData.getIcon())
                );
                payerItems.add(payerItem);
            }
        }

        payers.setValue(payerItems);
    }

    // Process participants/splits data
    private void processParticipants(Map<String, Object> splits, Map<String, Object> paidBy,
                                     Map<String, UserData> userDataMap) {
        List<ParticipantItem> participantItems = new ArrayList<>();
        String currency = transactionDetail.getValue().getCurrencyCode();

        // Find primary payer name for status text
        String primaryPayerName = "others";
        if (!paidBy.isEmpty()) {
            String primaryPayerId = paidBy.keySet().iterator().next();
            UserData primaryPayer = userDataMap.get(primaryPayerId);
            if (primaryPayer != null) {
                primaryPayerName = primaryPayer.getName();
            }
        }

        final String payerName = primaryPayerName;

        for (Map.Entry<String, Object> entry : splits.entrySet()) {
            String userId = entry.getKey();
            String amount = String.valueOf(entry.getValue());

            // Skip zero amounts
            try {
                if (Math.abs(Double.parseDouble(amount)) < 0.001) continue;
            } catch (NumberFormatException e) {
                continue;
            }

            UserData userData = userDataMap.get(userId);
            if (userData != null) {
                boolean isPayer = paidBy.containsKey(userId);
                String status = isPayer ? "Paid own share" : "Owes " + payerName;

                ParticipantItem participant = new ParticipantItem(
                        userData.getId(),
                        userData.getName(),
                        status,
                        amount,
                        currency,
                        IconUtils.getIconResourceId(userData.getIcon()),
                        !isPayer // isOwing is true if not a payer
                );
                participantItems.add(participant);
            }
        }

        participants.setValue(participantItems);
    }

    // Format date helper
    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "No date";

        Date date = timestamp.toDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    // Data classes
    public static class TransactionDetail {
        private final String title;
        private final String date;
        private final String currencyCode;
        private final String amount;
        private final int iconResource;

        public TransactionDetail(String title, String date, String currencyCode,
                                 String amount, int iconResource) {
            this.title = title;
            this.date = date;
            this.currencyCode = currencyCode;
            this.amount = amount;
            this.iconResource = iconResource;
        }

        public String getTitle() { return title; }
        public String getDate() { return date; }
        public String getCurrencyCode() { return currencyCode; }
        public String getAmount() { return amount; }
        public int getIconResource() { return iconResource; }
    }

    public static class PayerItem {
        private final String id;
        private final String name;
        private final String status;
        private final String amount;
        private final String currencyCode;
        private final int iconResource;

        public PayerItem(String id, String name, String status, String amount,
                         String currencyCode, int iconResource) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.amount = amount;
            this.currencyCode = currencyCode;
            this.iconResource = iconResource;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getAmount() { return amount; }
        public String getCurrencyCode() { return currencyCode; }
        public int getIconResource() { return iconResource; }
    }

    public static class ParticipantItem {
        private final String id;
        private final String name;
        private final String status;
        private final String amount;
        private final String currencyCode;
        private final int iconResource;
        private final boolean isOwing;

        public ParticipantItem(String id, String name, String status, String amount,
                               String currencyCode, int iconResource, boolean isOwing) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.amount = amount;
            this.currencyCode = currencyCode;
            this.iconResource = iconResource;
            this.isOwing = isOwing;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getAmount() { return amount; }
        public String getCurrencyCode() { return currencyCode; }
        public int getIconResource() { return iconResource; }
        public boolean isOwing() { return isOwing; }
    }

    private static class UserData {
        private final String id;
        private final String name;
        private final String icon;

        UserData(String id, String name, String icon) {
            this.id = id;
            this.name = name != null ? name : "Unknown";
            this.icon = icon;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
    }

    private interface UserDataCallback {
        void onUserDataFetched(Map<String, UserData> userDataMap);
    }
}