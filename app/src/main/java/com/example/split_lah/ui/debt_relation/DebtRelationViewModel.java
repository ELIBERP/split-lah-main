package com.example.split_lah.ui.debt_relation;

import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.split_lah.models.IconUtils;
import com.example.split_lah.models.TransactionLine;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DebtRelationViewModel extends ViewModel {
    private static final String TAG = "DebtRelationViewModel";
    private static final String COLLECTION_GROUPS = "permanent_grp";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTION_LINES = "transaction_lines";
    private static final String DEFAULT_CURRENCY = "SGD";

    private final FirebaseFirestore db;

    // State variables
    private String currentGroupId = null;
    private final MutableLiveData<List<Item>> mItems;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final LinkedList<TransactionLineData> transactionLines = new LinkedList<>();
    private List<String> groupMembers = new ArrayList<>();

    private boolean dataLoaded = false;
    private long lastLoadTimestamp = 0;
    // Cache period in milliseconds (5 minutes)
    private static final long CACHE_DURATION = 5 * 60 * 1000;
    private Map<String, UserData> userDataMap = new HashMap<>();
    private Map<String, Integer> userIdToIndex;
    private Map<Integer, String> indexToUserId;
    private ListenerRegistration transactionLinesListener;

    public List<String> getGroupMembers() {
        return new ArrayList<>(groupMembers); // Return a copy to prevent modification
    }

    public DebtRelationViewModel() {
        db = FirebaseFirestore.getInstance();
        mItems = new MutableLiveData<>();
        // Don't fetch items immediately, wait for group ID
    }

    public void setCurrentGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Log.w(TAG, "Ignoring empty group ID");
            completeWithEmptyState();
            return;
        }

        // Check if group actually changed
        boolean groupChanged = !groupId.equals(currentGroupId);

        // Update current group ID
        String oldGroupId = currentGroupId;
        this.currentGroupId = groupId;

        // Clear the cache and state if the group changed
        if (groupChanged) {
            Log.d(TAG, "Group changed from " + oldGroupId + " to " + groupId + ", resetting cache");
            dataLoaded = false;        // Important! Reset data loaded flag
            lastLoadTimestamp = 0;     // Reset timestamp to invalidate cache
            transactionLines.clear();  // Clear transaction lines
            groupMembers.clear();      // Clear group members
        }

        Log.d(TAG, "Setting group ID to: " + groupId +
                (groupChanged ? " (changed from " + oldGroupId + ")" : " (unchanged)"));

        // Set loading state to true when starting fetch
        isLoading.setValue(true);
        fetchItems();
    }

    public String getCurrentGroupId() {
        return currentGroupId;
    }

    public LiveData<List<Item>> getItems() {
        return mItems;
    }

    // Public getter for loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    private static class TransactionLineData {
        final String transactionId;
        final String paidBy;
        final String paidFor;
        final String amount;
        final String sum;
        final Timestamp dateCreated;

        TransactionLineData(String transactionId, String paidBy, String paidFor,
                            String amount, String sum, Timestamp dateCreated) {
            this.transactionId = transactionId;
            this.paidBy = paidBy;
            this.paidFor = paidFor;
            this.amount = amount;
            this.sum = sum;
            this.dateCreated = dateCreated;
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

    private static class PairData {
        final String paidBy;
        final String paidFor;
        final String sum;

        PairData(String paidBy, String paidFor, String sum) {
            this.paidBy = paidBy;
            this.paidFor = paidFor;
            this.sum = sum != null ? sum : "0";
        }
    }

    private static class PairDataInteger {
        final String paidBy;
        final String paidFor;
        final int sum;

        PairDataInteger(String paidBy, String paidFor, int sum) {
            this.paidBy = paidBy;
            this.paidFor = paidFor;
            this.sum = sum;
        }
    }

    public static class Item {
        public final String fromName;
        public final String toName;
        public final String amount;
        public final String currency;
        public final int iconPayer;
        public final int iconPayee;

        public Item(String fromName, String toName, String amount, String currency,
                    int iconPayer, int iconPayee) {
            this.fromName = fromName;
            this.toName = toName;
            this.amount = amount;
            this.currency = currency;
            this.iconPayer = iconPayer;
            this.iconPayee = iconPayee;
        }

        // Add these getter methods
        public String getPayer() {
            return fromName;
        }

        public String getPayee() {
            return toName;
        }
    }

    // STEP 1: Fetch group data and members
    private void fetchItems() {
        // Skip loading if data is fresh and we already have items
        List<Item> currentItems = mItems.getValue();
        boolean hasCachedItems = currentItems != null && !currentItems.isEmpty();
        boolean isCacheFresh = (System.currentTimeMillis() - lastLoadTimestamp) < CACHE_DURATION;

        if (dataLoaded && hasCachedItems && isCacheFresh) {
            Log.d(TAG, "Using cached debt relation items");
            // Just update loading state since we're using cached data
            isLoading.setValue(false);
            return;
        }

        // Set loading state
        isLoading.setValue(true);

        // Continue with normal loading from database
        // Add an additional check to ensure we have a valid group ID
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            Log.e(TAG, "Cannot fetch items: No group ID set");
            completeWithEmptyState();
            return;
        }

        Log.d(TAG, "Fetching data for group: " + currentGroupId);

        // Clear existing data
        transactionLines.clear();
        groupMembers.clear();

        db.collection(COLLECTION_GROUPS)
                .document(currentGroupId)
                .get()
                .addOnCompleteListener(groupTask -> {
                    if (!groupTask.isSuccessful()) {
                        Log.e(TAG, "Error fetching group: " + groupTask.getException());
                        completeWithEmptyState();
                        return;
                    }

                    if (!groupTask.getResult().exists()) {
                        Log.e(TAG, "Group not found: " + currentGroupId);
                        completeWithEmptyState();
                        return;
                    }

                    // Extract members list
                    groupMembers = extractMembers(groupTask.getResult());
                    Log.d(TAG, "Found " + groupMembers.size() + " group members");

                    // Continue to next step
                    setupTransactionLinesListener();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch group data: " + e.getMessage());
                    completeWithEmptyState();
                });
    }

    private List<String> extractMembers(DocumentSnapshot groupDoc) {
        List<String> members = new ArrayList<>();
        Object membersObj = groupDoc.get("members");

        if (membersObj instanceof List) {
            List<?> membersList = (List<?>) membersObj;
            for (Object member : membersList) {
                if (member instanceof String) {
                    members.add((String) member);
                }
            }
        }

        return members;
    }

    // STEP 2: Set up transaction lines listener
    private void setupTransactionLinesListener() {
        db.collection(COLLECTION_GROUPS)
                .document(currentGroupId)
                .collection(COLLECTION_TRANSACTION_LINES)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for transactions: " + error);
                        completeWithEmptyState();
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.d(TAG, "No transaction lines found");
                        completeWithEmptyState();
                        return;
                    }

                    transactionLines.clear();
                    for (DocumentSnapshot lineDoc : snapshot.getDocuments()) {
                        TransactionLineData line = createTransactionLine(lineDoc);
                        if (line != null) {
                            transactionLines.add(line);
                        }
                    }

                    if (transactionLines.isEmpty()) {
                        Log.d(TAG, "No valid transaction lines found");
                        mItems.postValue(new ArrayList<>());
                        return;
                    }

                    Log.d(TAG, "Loaded " + transactionLines.size() + " transaction lines");
                    collectUniqueUserIds();
                });
    }

    private void completeWithEmptyState() {
        isLoading.postValue(false);
        mItems.postValue(new ArrayList<>());
    }

    private TransactionLineData createTransactionLine(DocumentSnapshot doc) {
        String transactionId = doc.getString("transactions_id");
        String paidBy = doc.getString("paid_by");
        String paidFor = doc.getString("paid_for");
        String amount = doc.getString("amount");
        String sum = doc.getString("sum");
        Timestamp dateCreated = doc.getTimestamp("created_at");

        // Validate required fields
        if (paidBy == null || paidFor == null || sum == null) {
            Log.w(TAG, "Skipping invalid transaction line: " + doc.getId());
            return null;
        }

        return new TransactionLineData(
                transactionId, paidBy, paidFor, amount, sum, dateCreated);
    }

    // STEP 3: Collect unique user IDs from both members and transactions
    private void collectUniqueUserIds() {
        Set<String> uniqueUserIds = new HashSet<>(groupMembers);

        // Add users from transaction lines
        for (TransactionLineData line : transactionLines) {
            if (line.paidBy != null) uniqueUserIds.add(line.paidBy);
            if (line.paidFor != null) uniqueUserIds.add(line.paidFor);
        }

        if (uniqueUserIds.isEmpty()) {
            Log.w(TAG, "No users to fetch");
            return;
        }

        Log.d(TAG, "Found " + uniqueUserIds.size() + " unique user IDs");
        fetchUserData(uniqueUserIds);
    }

    // STEP 4: Fetch user data for all unique IDs
    private void fetchUserData(Set<String> uniqueUserIds) {
        final int[] processedCount = {0};
        final int totalUsers = uniqueUserIds.size();

        for (String userId : uniqueUserIds) {
            db.collection(COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String firstName = task.getResult().getString("first_name");
                            String icon = task.getResult().getString("icon");
                            String defaultCurrency = task.getResult().getString("default_currency");

                            userDataMap.put(userId, new UserData(firstName, icon, defaultCurrency));
                            Log.d(TAG, "Loaded user: " + userId + " (" + firstName + ")");
                        } else {
                            userDataMap.put(userId, new UserData("User " + userId, null, null));
                            Log.w(TAG, "User not found: " + userId);
                        }

                        processedCount[0]++;
                        if (processedCount[0] == totalUsers) {
                            Log.d(TAG, "Loaded all " + totalUsers + " users");
                            processTransactionData(userDataMap);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user " + userId + ": " + e.getMessage());

                        // Still count this user to avoid getting stuck
                        userDataMap.put(userId, new UserData("User " + userId, null, null));
                        processedCount[0]++;

                        if (processedCount[0] == totalUsers) {
                            processTransactionData(userDataMap);
                        }
                    });
        }
    }

    // STEP 5: Process transaction data with user data
    private void processTransactionData(Map<String, UserData> userDataMap) {
        if (transactionLines.isEmpty()) {
            Log.w(TAG, "No transactions to process");
            mItems.postValue(new ArrayList<>());
            return;
        }

        // Find unique pairs
        Map<String, PairData> uniquePairs = findFirstPairInstances();

        if (uniquePairs.isEmpty()) {
            Log.d(TAG, "No debt relations found");
            mItems.postValue(new ArrayList<>());
            return;
        }

        // Convert to integer cents
        Map<String, PairDataInteger> uniquePairsInt = convertToIntegerPairs(uniquePairs);

        if (uniquePairsInt.isEmpty()) {
            Log.d(TAG, "No valid debt amounts found");
            mItems.postValue(new ArrayList<>());
            return;
        }

        // Process debt simplification
        runSimplifyDebts(userDataMap, uniquePairsInt);
    }

    private Map<String, PairData> findFirstPairInstances() {
        Map<String, PairData> uniquePairs = new HashMap<>();

        // Create a copy of the transaction lines
        List<TransactionLineData> sortedLines = new ArrayList<>(transactionLines);

        // Sort by timestamp (newest first to maintain existing behavior)
        sortedLines.sort((a, b) -> {
            if (a.dateCreated == null && b.dateCreated == null) return 0;
            if (a.dateCreated == null) return 1; // Null timestamps go last
            if (b.dateCreated == null) return -1;
            return b.dateCreated.compareTo(a.dateCreated); // Descending order (newest first)
        });

        Log.d(TAG, "Processing " + sortedLines.size() + " transaction lines by timestamp (newest first)");

        for (TransactionLineData line : sortedLines) {
            // Skip invalid entries
            if (line.paidBy == null || line.paidFor == null) {
                continue;
            }

            // Create a consistent key for this pair
            String pairKey = createPairKey(line.paidBy, line.paidFor);

            // If this is the first time we've seen this pair, record it
            // (which will be the most recent transaction for this pair)
            if (!uniquePairs.containsKey(pairKey)) {
                PairData pairData = new PairData(line.paidBy, line.paidFor, line.sum);
                uniquePairs.put(pairKey, pairData);

                // Log the timestamp for debugging
                String timestamp = line.dateCreated != null ?
                        line.dateCreated.toDate().toString() : "null";
                Log.d(TAG, "Using transaction from " + timestamp + " for pair " + pairKey);
            }
        }

        Log.d(TAG, "Found " + uniquePairs.size() + " unique debt relations");
        return uniquePairs;
    }

    private String createPairKey(String idA, String idB) {
        // Ensure consistent ordering regardless of which user is which
        return idA.compareTo(idB) < 0 ? idA + "<->" + idB : idB + "<->" + idA;
    }

    private Map<String, PairDataInteger> convertToIntegerPairs(Map<String, PairData> pairs) {
        Map<String, PairDataInteger> result = new HashMap<>();
    
        for (Map.Entry<String, PairData> entry : pairs.entrySet()) {
            PairData pair = entry.getValue();
            try {
                // Extract the numeric part if the sum contains currency code
                String numericSum = pair.sum.replaceAll("[^0-9.]", "");
                
                // Skip if we ended up with an empty string
                if (numericSum.isEmpty()) {
                    Log.e(TAG, "No numeric value found in sum: " + pair.sum);
                    continue;
                }
                
                double doubleSum = Double.parseDouble(numericSum);
                int intSum = (int) Math.round(doubleSum * 100);
    
                result.put(entry.getKey(), new PairDataInteger(
                        pair.paidBy, pair.paidFor, intSum));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing sum: " + pair.sum, e);
                // Continue with next pair instead of breaking the whole process
            }
        }
    
        return result;
    }

    // STEP 6: Run the debt simplification algorithm
    private void runSimplifyDebts(Map<String, UserData> userDataMap, Map<String, PairDataInteger> uniquePairsInt) {
        // Check for empty data
        if (uniquePairsInt.isEmpty()) {
            Log.d(TAG, "No debts to simplify");
            mItems.postValue(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Starting debt simplification with " + uniquePairsInt.size() + " relations");

        // Extract all unique user IDs
        Set<String> uniqueUserIds = extractUniqueUserIds(uniquePairsInt);

        // Create mappings between indices and user IDs
        List<String> userIdList = new ArrayList<>(uniqueUserIds);
        Collections.sort(userIdList);

        Map<String, Integer> userIdToIndex = new HashMap<>();
        Map<Integer, String> indexToUserId = new HashMap<>();

        // Create the array with a padding at index 0
        String[] personNames = createPersonNamesArray(userIdList, userDataMap, userIdToIndex, indexToUserId);

        // Initialize solver and add edges
        Dinics solver = initializeSolver(personNames, uniquePairsInt, userIdToIndex);

        // Run simplification algorithm
        SimplifyDebts simplifier = new SimplifyDebts();
        Dinics resultSolver = simplifier.simplifyTransactions(solver);

        // Extract simplified debts
        List<Dinics.Edge> simplifiedDebts = extractSimplifiedDebts(resultSolver);

        if (simplifiedDebts.isEmpty()) {
            Log.d(TAG, "No simplified debts found");
            mItems.postValue(new ArrayList<>());
            return;
        }

        // Update UI with the results
        updateDebtsItems(simplifiedDebts, personNames, userDataMap, indexToUserId);
    }

    private Set<String> extractUniqueUserIds(Map<String, PairDataInteger> uniquePairsInt) {
        Set<String> uniqueUserIds = new HashSet<>();
        for (PairDataInteger pair : uniquePairsInt.values()) {
            uniqueUserIds.add(pair.paidBy);
            uniqueUserIds.add(pair.paidFor);
        }
        return uniqueUserIds;
    }

    private String[] createPersonNamesArray(List<String> userIdList,
                                            Map<String, UserData> userDataMap,
                                            Map<String, Integer> userIdToIndex,
                                            Map<Integer, String> indexToUserId) {
        String[] personNames = new String[userIdList.size()];

        for (int i = 0; i < userIdList.size(); i++) {
            String userId = userIdList.get(i);

            userIdToIndex.put(userId, i);
            indexToUserId.put(i, userId);

            // Get user's first name
            UserData userData = userDataMap.get(userId);
            personNames[i] = userData != null ? userData.firstName : "User " + userId;
        }

        return personNames;
    }

    private Dinics initializeSolver(String[] personNames,
                                    Map<String, PairDataInteger> uniquePairsInt,
                                    Map<String, Integer> userIdToIndex) {
        Dinics solver = new Dinics(personNames.length, personNames);

        // Add edges representing debts (paidFor owes paidBy)
        for (PairDataInteger pair : uniquePairsInt.values()) {
            int from = userIdToIndex.get(pair.paidFor); // Debtor
            int to = userIdToIndex.get(pair.paidBy);   // Creditor
            solver.addEdge(from, to, pair.sum);
        }

        return solver;
    }

    private List<Dinics.Edge> extractSimplifiedDebts(Dinics solver) {
        List<Dinics.Edge> simplifiedDebts = new ArrayList<>();
        for (Dinics.Edge edge : solver.getEdges()) {
            if (edge.capacity > 0) {
                simplifiedDebts.add(edge);
            }
        }
        return simplifiedDebts;
    }

    // Update Items with simplified debts
    private void updateDebtsItems(List<Dinics.Edge> simplifiedDebts, String[] personNames,
                               Map<String, UserData> userDataMap, Map<Integer, String> indexToUserId) {
        List<Item> items = new ArrayList<>();

        for (Dinics.Edge edge : simplifiedDebts) {
            if (edge.capacity <= 0) continue;

            // Get the user IDs using the edge indices
            String fromUserId = indexToUserId.get(edge.from);
            String toUserId = indexToUserId.get(edge.to);

            if (fromUserId == null || toUserId == null) {
                Log.e(TAG, "Missing user ID for index: from=" + edge.from + ", to=" + edge.to);
                continue;
            }

            // Get user data
            UserData fromUserData = userDataMap.get(fromUserId);
            UserData toUserData = userDataMap.get(toUserId);

            // Names for display
            String fromName = personNames[edge.from];
            String toName = personNames[edge.to];

            // Format amount
            double amount = edge.capacity / 100.0;

            // Get currency
            String currency = DEFAULT_CURRENCY;
            if (toUserData != null && toUserData.defaultCurrency != null &&
                    !toUserData.defaultCurrency.isEmpty()) {
                currency = toUserData.defaultCurrency;
            }

            // Get icon resources
            int iconPayer = IconUtils.getIconResourceId(fromUserData != null ? fromUserData.icon : null);
            int iconPayee = IconUtils.getIconResourceId(toUserData != null ? toUserData.icon : null);

            // Create item for UI
            Item item = new Item(
                    fromName,
                    toName,
                    String.format("%.2f", amount),
                    currency,
                    iconPayer,
                    iconPayee
            );

            items.add(item);

            Log.d(TAG, String.format("Simplified debt: %s owes %s %s%.2f",
                    fromName, toName, currency, amount));
        }

        // At the end, update both loading state and items
        dataLoaded = true;
        lastLoadTimestamp = System.currentTimeMillis();
        isLoading.postValue(false);
        mItems.postValue(items);
        Log.d(TAG, "UI updated with " + items.size() + " simplified debts");
    }

    public void settleDebt(String fromName, String toName, String amount, String currency) {
        Log.d(TAG, "Settling Debt");

        if (currentGroupId == null || currentGroupId.isEmpty()) {
            Log.e(TAG, "Cannot settle debt: No group ID set");
            return;
        }

        // Find the user IDs from the names
        String fromUserId = null;
        String toUserId = null;

        // Find the user IDs based on names in userDataMap
        for (Map.Entry<String, UserData> entry : userDataMap.entrySet()) {
            UserData userData = entry.getValue();
            String userId = entry.getKey();

            if (userData != null) {
                if (userData.firstName.equals(fromName)) {
                    fromUserId = userId;
                }
                if (userData.firstName.equals(toName)) {
                    toUserId = userId;
                }
            }
        }

        if (fromUserId == null || toUserId == null) {
            Log.e(TAG, "Cannot settle debt: User IDs not found for names: " + fromName + ", " + toName);
            return;
        }

        // paid by
        String finalFromUserId = fromUserId;
        HashMap<String, String> paidBy = new HashMap<String, String>(){{
            put(finalFromUserId, amount);
        }};

        // final split
        String finalToUserId = toUserId;
        HashMap<String, String> finalSplits = new HashMap<String, String>(){{
            put(finalToUserId, amount);
        }};

        HashMap<String, Object> billData = new HashMap<>();
        billData.put("title", "Repayment");
        billData.put("amount", amount);
        billData.put("paid_by", paidBy);
        billData.put("split_type", "Transfer");
        billData.put("splits", finalSplits);
        billData.put("created_at", FieldValue.serverTimestamp());
        billData.put("currency_code", currency);
        billData.put("icon", "paid");
        billData.put("repeating_transaction", false);
        billData.put("description", "");
        billData.put("exchange_rate", "");

        // For permanent groups, save bill to Firestore
        String path = "permanent_grp/" + currentGroupId + "/transactions";

        // Save bill to Firestore
        db.collection(path).add(billData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction saved successfully!");
//                    Toast.makeText(itemView.getContext(), "Bill saved successfully!", Toast.LENGTH_SHORT).show();
                    TransactionLine tLine = new TransactionLine(
                            amount,
                            finalFromUserId,
                            finalToUserId,
                            amount,
                            documentReference.getId(),
                            currentGroupId
                    );
                    addTransactionLine(tLine);
                })
                .addOnFailureListener(e -> {
//                    Toast.makeText(itemView.getContext(), "Error saving bill", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error: ", e);
                });
    }

    public void addTransactionLine(TransactionLine transactionLine) {
        transactionLine.calculateSum(finalSum -> {
            // Create a map representation of the transaction line for Firestore
            HashMap<String, Object> transactionLineData = new HashMap<>();
            transactionLineData.put("amount", transactionLine.getAmount());
            transactionLineData.put("created_at", FieldValue.serverTimestamp());
            transactionLineData.put("paid_by", transactionLine.getPaid_by());
            transactionLineData.put("paid_for", transactionLine.getPaid_for());
            transactionLineData.put("sum", finalSum); // Use the calculated sum here
            transactionLineData.put("transactions_id", transactionLine.getTransactions_id());

            // Construct the path to the "transaction_lines" subcollection
            String path2 = "permanent_grp/" + currentGroupId + "/transaction_lines";

            // save to Firestore after calculation is complete
            db.collection(path2)
                    .add(transactionLineData)
                    .addOnSuccessListener(documentReference -> {

                        // Only finish the activity after the LAST transaction line is saved
                        // This ensures all transaction lines are saved before returning
                        if (transactionLine.isLastTransaction()) {
//                            Toast.makeText(itemView.getContext(), "Bill saved successfully!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Transaction line saved successfully with sum: " + finalSum);}
                    })
                    .addOnFailureListener(e -> {
//                        Toast.makeText(itemView.getContext(), "Error saving transaction line", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error: ", e);
                    });
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (transactionLinesListener != null) {
            transactionLinesListener.remove();
            transactionLinesListener = null;
        }
    }

    public void forceRefresh() {
        dataLoaded = false;
        lastLoadTimestamp = 0;
        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            fetchItems();
        }
    }
}