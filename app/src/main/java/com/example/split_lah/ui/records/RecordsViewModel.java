package com.example.split_lah.ui.records;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.split_lah.R;
import com.example.split_lah.models.IconUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecordsViewModel extends ViewModel {
    // Constants
    private static final String TAG = "RecordsViewModel";
    private static final String COLLECTION_GROUPS = "permanent_grp";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";

    private static final long CACHE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutes

    // State variables
    private final MutableLiveData<List<Item>> mItems;
    private final MutableLiveData<List<ListItem>> mAllItems; // All items including headers
    private final FirebaseFirestore db;
    private String currentGroupId = null;

    // Cache variables
    private long lastFetchTimestamp = 0;
    private List<TransactionData> cachedTransactions = null;
    private Map<String, UserData> cachedUserData = new HashMap<>();
    private ListenerRegistration transactionListener = null;

    // Data classes
    private static class TransactionData {
        final String id;
        final String title;
        final String currencyCode;
        final String amount;
        final Timestamp createdAt;
        final String iconName;
        final Map<String, Object> splits;
        final Map<String, Object> paidBy;

        TransactionData(String id, String title, String currencyCode, String amount,
                        Timestamp createdAt, String iconName, Map<String, Object> splits,
                        Map<String, Object> paidBy) {
            this.id = id;
            this.title = title;
            this.currencyCode = currencyCode;
            this.amount = amount;
            this.createdAt = createdAt;
            this.iconName = iconName;
            this.splits = splits;
            this.paidBy = paidBy;
        }
    }

    private static class UserData {
        final String firstName;
        final String icon;
        final String defaultCurrency;

        UserData(String firstName, String icon, String defaultCurrency) {
            this.firstName = firstName != null ? firstName : "Unknown";
            this.icon = icon;
            this.defaultCurrency = defaultCurrency;
        }
    }

    // Constructor
    public RecordsViewModel() {
        mItems = new MutableLiveData<>();
        mAllItems = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
    }

    // Public methods
    public void setCurrentGroupId(String groupId) {
        // Handle null groupId or same group
        if (groupId == null || (currentGroupId != null && groupId.equals(currentGroupId))) {
            return;
        }

        // Group changed
        Log.d(TAG, "Changing group from " + currentGroupId + " to " + groupId);
        this.currentGroupId = groupId;

        // Clear existing data immediately to prevent stale data display
        mItems.setValue(new ArrayList<>());
        mAllItems.setValue(new ArrayList<>());

        // Different group, invalidate cache
        invalidateCache();

        // Setup realtime updates for the new group
        setupRealtimeTransactionUpdates();

        // Fetch new data
        fetchItemsWithCaching();
    }

    public LiveData<List<Item>> getItems() {
        return mItems;
    }

    public LiveData<List<ListItem>> getAllItems() {
        return mAllItems;
    }

    // Cache management methods
    public void invalidateCache() {
        Log.d(TAG, "Invalidating cache");
        cachedTransactions = null;
        lastFetchTimestamp = 0;
    }

    private boolean isCacheValid() {
        if (cachedTransactions == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastFetchTimestamp) < CACHE_EXPIRATION_MS;
    }

    // Setup realtime listener to invalidate cache on changes
    private void setupRealtimeTransactionUpdates() {
        // Remove existing listener if any
        if (transactionListener != null) {
            transactionListener.remove();
            transactionListener = null;
        }

        if (currentGroupId == null || currentGroupId.isEmpty()) {
            return;
        }

        Log.d(TAG, "Setting up realtime listener for group: " + currentGroupId);

        // Listen for changes to invalidate cache
        transactionListener = db.collection(COLLECTION_GROUPS)
                .document(currentGroupId)
                .collection(COLLECTION_TRANSACTIONS)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    // Explicitly handle empty collections
                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "Snapshot is empty or null - clearing transaction data");
                        cachedTransactions = new ArrayList<>();
                        lastFetchTimestamp = System.currentTimeMillis();
                        mItems.postValue(new ArrayList<>());
                        mAllItems.postValue(new ArrayList<>());
                        return;
                    }

                    // ALWAYS fetch fresh data on ANY change
                    Log.d(TAG, "Transaction data changed - fetching fresh data");
                    cachedTransactions = null; // Force cache refresh
                    fetchItemsWithCaching();
                });
    }

    // Extract all member IDs from transactions
    private Set<String> extractMemberIds(List<TransactionData> transactions) {
        Set<String> allMemberIds = new HashSet<>();
        for (TransactionData transaction : transactions) {
            if (transaction.splits != null) {
                for (Map.Entry<String, Object> entry : transaction.splits.entrySet()) {
                    // Check if the split value is non-zero before adding the member
                    String memberId = entry.getKey();
                    Object splitValue = entry.getValue();

                    if (isNonZeroValue(splitValue)) {
                        allMemberIds.add(memberId);
                    }
                }
            }
        }
        return allMemberIds;
    }
    // Helper method to check if a value is non-zero
    private boolean isNonZeroValue(Object value) {
        if (value == null) return false;

        try {
            if (value instanceof String) {
                double doubleValue = Double.parseDouble((String) value);
                return Math.abs(doubleValue) > 0.001; // Use small epsilon for floating point comparison
            } else if (value instanceof Number) {
                double doubleValue = ((Number) value).doubleValue();
                return Math.abs(doubleValue) > 0.001;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing split value: " + value, e);
        }

        return false; // Consider non-numeric values as zero
    }

    // Modified fetch method with caching
    private void fetchItemsWithCaching() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            Log.e(TAG, "Cannot fetch items: No group ID set");
            mItems.setValue(new ArrayList<>());
            mAllItems.setValue(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Checking cache for group: " + currentGroupId);

        if (isCacheValid()) {
            Log.d(TAG, "Using cached data for transactions");
            processTransactionMembers(cachedTransactions, extractMemberIds(cachedTransactions));
            return;
        }

        // Cache invalid or doesn't exist, fetch from Firestore
        fetchItems();
    }

    // STEP 1: Fetch group data
    private void fetchItems() {
        // Add an additional check to ensure we have a valid group ID
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            Log.e(TAG, "Cannot fetch items: No group ID set");
            mItems.setValue(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Fetching records for group: " + currentGroupId);

        db.collection(COLLECTION_GROUPS)
                .document(currentGroupId)
                .get()
                .addOnCompleteListener(groupTask -> {
                    if (!groupTask.isSuccessful() || !groupTask.getResult().exists()) {
                        mItems.setValue(new ArrayList<>());
                        mAllItems.setValue(new ArrayList<>());
                        return;
                    }

                    // Group exists, continue to fetch transactions
                    fetchTransactions();
                });
    }

    // STEP 2: Fetch transactions
    private void fetchTransactions() {
        db.collection(COLLECTION_GROUPS)
                .document(currentGroupId)
                .collection(COLLECTION_TRANSACTIONS)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(transactionsTask -> {
                    if (!transactionsTask.isSuccessful()) {
                        Log.e(TAG, "Error fetching transactions", transactionsTask.getException());
                        mItems.setValue(new ArrayList<>());
                        mAllItems.setValue(new ArrayList<>());
                        return;
                    }

                    // Handle empty transaction collection explicitly
                    if (transactionsTask.getResult().isEmpty()) {
                        Log.d(TAG, "No transactions found for group: " + currentGroupId);
                        // Update cache with empty list
                        cachedTransactions = new ArrayList<>();
                        lastFetchTimestamp = System.currentTimeMillis();
                        // Update UI with empty lists
                        mItems.setValue(new ArrayList<>());
                        mAllItems.setValue(new ArrayList<>());
                        return;
                    }

                    // Process transactions and extract all member IDs
                    List<TransactionData> transactionDataList = new ArrayList<>();
                    Set<String> allMemberIds = new HashSet<>();

                    for (QueryDocumentSnapshot doc : transactionsTask.getResult()) {
                        TransactionData transactionData = createTransactionData(doc);
                        if (transactionData != null) {
                            transactionDataList.add(transactionData);

                            // Collect member IDs
                            if (transactionData.splits != null) {
                                // Only add members with non-zero splits
                                for (Map.Entry<String, Object> entry : transactionData.splits.entrySet()) {
                                    if (isNonZeroValue(entry.getValue())) {
                                        allMemberIds.add(entry.getKey());
                                    }
                                }
                            }
                        }
                    }

                    // Also handle the case where we have documents but none are valid
                    if (transactionDataList.isEmpty()) {
                        Log.d(TAG, "No valid transactions found for group: " + currentGroupId);
                        // Update cache with empty list
                        cachedTransactions = new ArrayList<>();
                        lastFetchTimestamp = System.currentTimeMillis();
                        // Update UI with empty lists
                        mItems.setValue(new ArrayList<>());
                        mAllItems.setValue(new ArrayList<>());
                        return;
                    }

                    // Update cache
                    cachedTransactions = transactionDataList;
                    lastFetchTimestamp = System.currentTimeMillis();
                    Log.d(TAG, "Updated transaction cache with " + transactionDataList.size() + " items");

                    // Process the data
                    processTransactionMembers(transactionDataList, allMemberIds);
                });
    }

    // Helper to create transaction data from document
    private TransactionData createTransactionData(DocumentSnapshot doc) {
        // Log the document data to debug field mismatches
        Log.d(TAG, "Document data: " + doc.getData());

        String id = doc.getId();
        String title = doc.getString("title");
        String currencyCode = doc.getString("currency_code");
        String amount = doc.getString("amount");
        Timestamp createdAt = doc.getTimestamp("created_at");
        String iconName = doc.getString("icon");
        Map<String, Object> splits = (Map<String, Object>) doc.get("splits");
        Map<String, Object> paidBy = (Map<String, Object>) doc.get("paid_by");

        // Log extracted fields
        Log.d(TAG, "Transaction data extracted: " +
                "\n id: " + id +
                "\n title: " + title +
                "\n currencyCode: " + currencyCode +
                "\n amount: " + amount +
                "\n date: " + (createdAt != null ? createdAt.toDate() : "null") +
                "\n iconName: " + iconName +
                "\n splits: " + (splits != null ? "Map with " + splits.size() + " members " + splits.keySet() : "null") +
                "\n paidBy: " + (paidBy != null ? "Map with " + paidBy.size() + " payers " + paidBy.keySet() : "null"));

        // Validate required fields
        if (title == null || amount == null || createdAt == null) {
            Log.w(TAG, "Skipping invalid transaction: " + id);
            return null;
        }

        return new TransactionData(
                id, title, currencyCode, amount, createdAt, iconName, splits, paidBy);
    }

    // STEP 3: Process transaction members
    private void processTransactionMembers(List<TransactionData> transactions, Set<String> allMemberIds) {
        // Fetch user data for members
        fetchUserDataForMembers(allMemberIds, userDataMap -> {
            // Create items from transactions
            List<Item> items = createItemsFromTransactions(transactions, userDataMap);

            // Update the items LiveData
            mItems.postValue(items);

            // Convert normal items to ListItem objects for mAllItems
            List<ListItem> listItems = new ArrayList<>();
            for (Item item : items) {
                // Assuming Item implements ListItem or can be cast to it
                listItems.add((ListItem) item);
            }

            // Update the allItems LiveData with the converted list
            mAllItems.postValue(listItems);

            Log.d(TAG, "Loaded " + items.size() + " transactions");
        });
    }

    // STEP 4: Create UI items from transaction data
    private List<Item> createItemsFromTransactions(List<TransactionData> transactions, Map<String, UserData> userDataMap) {
        List<Item> items = new ArrayList<>();

        for (TransactionData transData : transactions) {
            // Format date
            String formattedDate = formatDate(transData.createdAt);

            // Get icon resource
            int iconResource = IconUtils.getIconResourceId(transData.iconName);

            // Process members involved to get member icons
            List<Integer> memberIcons = new ArrayList<>();
            List<String> memberNames = new ArrayList<>();

            if (transData.splits != null) {
                for (Map.Entry<String, Object> entry : transData.splits.entrySet()) {
                    String memberId = entry.getKey();
                    Object splitValue = entry.getValue();

                    // Skip members with zero split values
                    if (!isNonZeroValue(splitValue)) {
                        continue;
                    }

                    UserData userData = userDataMap.get(memberId);
                    if (userData != null) {
                        memberNames.add(userData.firstName);

                        int iconId = userData.icon != null ?
                                IconUtils.getIconResourceId(userData.icon) : R.drawable.giraffe;
                        memberIcons.add(iconId);
                    }
                }
            }

            // Process payer information
            String payerText;
            if (transData.paidBy == null || transData.paidBy.isEmpty()) {
                payerText = "Unknown";
            } else if (transData.paidBy.size() == 1) {
                // Only one payer, show their name
                String payerId = transData.paidBy.keySet().iterator().next();
                UserData payerData = userDataMap.get(payerId);
                payerText = payerData != null ? payerData.firstName : "Unknown";
            } else {
                // Multiple payers, show the count
                payerText = transData.paidBy.size() + " people";
            }

            // Create item with all the fetched data
            Item item = new Item(
                    transData.id,  // Include the transaction ID
                    transData.title,
                    formattedDate,
                    transData.amount,
                    transData.currencyCode,
                    iconResource,
                    memberIcons,
                    payerText
            );

            items.add(item);

            Log.d(TAG, "Created item: " + transData.title + ", amount: " +
                    transData.currencyCode + transData.amount + ", payer: " + payerText);
        }

        return items;
    }

    // Helper method to fetch user data for all members
    private void fetchUserDataForMembers(Set<String> memberIds, OnUserDataFetchedListener listener) {
        if (memberIds.isEmpty()) {
            listener.onUserDataFetched(new HashMap<>());
            return;
        }

        // Filter member IDs that are not in cache yet
        Set<String> uncachedMemberIds = new HashSet<>();
        Map<String, UserData> resultMap = new HashMap<>(cachedUserData); // Start with cached data

        for (String userId : memberIds) {
            if (!cachedUserData.containsKey(userId)) {
                uncachedMemberIds.add(userId);
            }
        }

        // If all members are cached, return immediately
        if (uncachedMemberIds.isEmpty()) {
            Log.d(TAG, "Using cached user data for all members");
            listener.onUserDataFetched(resultMap);
            return;
        }

        // Fetch only uncached user data
        final int[] processedCount = {0};
        final int totalUsers = uncachedMemberIds.size();
        Log.d(TAG, "Fetching user data for " + totalUsers + " uncached members");

        for (String userId : uncachedMemberIds) {
            db.collection(COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String firstName = task.getResult().getString("first_name");
                            String icon = task.getResult().getString("icon");
                            String defaultCurrency = task.getResult().getString("default_currency");

                            UserData userData = new UserData(firstName, icon, defaultCurrency);
                            resultMap.put(userId, userData);
                            cachedUserData.put(userId, userData); // Update cache
                        } else {
                            UserData userData = new UserData("User " + userId, null, null);
                            resultMap.put(userId, userData);
                            cachedUserData.put(userId, userData); // Update cache with default
                        }

                        processedCount[0]++;
                        if (processedCount[0] == totalUsers) {
                            listener.onUserDataFetched(resultMap);
                        }
                    })
                    .addOnFailureListener(e -> {
                        processedCount[0]++;
                        if (processedCount[0] == totalUsers) {
                            listener.onUserDataFetched(resultMap);
                        }
                    });
        }
    }

    @Override
    public void onCleared() {
        super.onCleared();
        if (transactionListener != null) {
            transactionListener.remove();
        }
    }

    public void forceRefresh() {
        Log.d(TAG, "Force refreshing records data");
        invalidateCache();
        fetchItemsWithCaching();
    }

    // Interface for callback when user data is fetched
    interface OnUserDataFetchedListener {
        void onUserDataFetched(Map<String, UserData> userDataMap);
    }

    // Helper method to format date
    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "No date";
        }

        Date date = timestamp.toDate();
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return formatter.format(date);
    }
}