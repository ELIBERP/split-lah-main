package com.example.split_lah.ui.group;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.split_lah.R;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;


import java.util.*;

/**
 * ViewModel for managing the logic and state of the "Add Group" page.
 */
public class BaseGroupViewModel extends ViewModel {

    // Create a LiveData object with the default values
    private final MutableLiveData<String> groupName = new MutableLiveData<>("");
    private final MutableLiveData<List<String>> availableCurrencies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> fullName = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> newGroupId = new MutableLiveData<>();
    private final MutableLiveData<Map<String, User>> usersMap = new MutableLiveData<>(new HashMap<>());
    private OnGroupCreatedListener groupCreatedListener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public LiveData<String> getGroupName() {
        return groupName;
    }

    public LiveData<String> getFullName() {
        return fullName;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<List<String>> getAvailableCurrencies() {
        return availableCurrencies;
    }

    public LiveData<String> getNewGroupId() {
        return newGroupId;
    }

    public LiveData<Integer> getUserIcon() {
        MutableLiveData<Integer> iconResource = new MutableLiveData<>();

        // Fetch the current user's ID
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        // Retrieve the current user's nickname
        Map<String, User> currentUsers = usersMap.getValue();
        if (currentUsers != null && currentUsers.containsKey(Objects.requireNonNull(firebaseUser).getDisplayName())) {
            User user = currentUsers.get(firebaseUser.getDisplayName());
            if (user != null) {
                // Get the icon resource ID using the getIconResourceId method
                int iconResId = IconUtils.getIconResourceId(user.getIcon());
                iconResource.setValue(iconResId);
            } else {
                iconResource.setValue(R.drawable.giraffe);
            }
        } else {
            // If user is not found in usersMap, fetch them from Firestore
            db.collection("users").document(Objects.requireNonNull(firebaseUser).getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            // Create the user object from Firestore data
                            String icon = doc.getString("icon");
                            User user = new User(firebaseUser.getUid(), doc.getString("first_name"), doc.getString("last_name"), "1", icon);
                            int iconResId = IconUtils.getIconResourceId(icon);
                            iconResource.setValue(iconResId);
                        } else {
                            iconResource.setValue(R.drawable.giraffe);
                        }
                    })
                    .addOnFailureListener(e -> {
                        iconResource.setValue(R.drawable.giraffe);
                    });
        }

        return iconResource;
    }

    public void setGroupName(String name) {
        groupName.setValue(name);
    }

    /**
     * Loads a list of currency codes from Firestore and updates LiveData
     * Defaults to "SGD" at the top.
     */
    public void loadCurrenciesFromFirestore() {
        db.collection("countries_info").get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> currencySet = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String code = doc.getString("currency_code");
                        if (code != null && !code.trim().isEmpty()) {
                            currencySet.add(code);
                        }
                    }
                    List<String> currencies = new ArrayList<>(currencySet);
                    Collections.sort(currencies);

                    if (currencies.remove("SGD")) currencies.add(0, "SGD");
                    availableCurrencies.setValue(currencies);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading currencies", e);
                    errorMessage.setValue("Error loading currencies. Please try again.");
                });
    }

    /**
     * Fetches the current user's full name from Firestore and updates LiveData
     * This is used to auto-fill the first member in the group
     */
    public void getLoggedInUserFullName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String first = doc.getString("first_name");
                    String last = doc.getString("last_name");
                    if (first != null && last != null) {
                        fullName.setValue(first + " " + last);
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue("Error loading name. Please try again."));
    }

    /**
     * Saves a new group to Firestore with the specified details
     *
     * @param groupName Name of the group
     * @param currency  Selected currency
     * @param nicknames Set of nicknames to be matched with user accounts
     */
    public void savePermanentGroup(String groupName, String currency, Set<String> nicknames) {
        if (groupName.isEmpty() || currency.isEmpty()) {
            errorMessage.setValue("Please fill in all required fields");
            return;
        }

        newGroupId.setValue(null);

        String currentUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        fetchUIDsForNicknames(nicknames, result -> {
            Set<String> allUids = new HashSet<>(result.uids);
            allUids.add(currentUid);

            // store group details in Firestore
            Map<String, Object> group = new HashMap<>();
            group.put("group_name", groupName);
            group.put("currency_code", currency);
            group.put("created_at", FieldValue.serverTimestamp());
            group.put("owner", currentUid);
            group.put("members", new ArrayList<>(allUids));
            group.put("ghost_members", result.ghostMembers);

            db.collection("permanent_grp").add(group)
                    .addOnSuccessListener(docRef -> {
                        newGroupId.setValue(docRef.getId());
                        if (groupCreatedListener != null) {
                            groupCreatedListener.onGroupCreated(); // trigger refresh
                        }
                    })
                    .addOnFailureListener(e -> errorMessage.setValue("Failed to create group: " + e.getMessage()));
        });
    }

    /**
     * Fetches Firebase user UIDs based on the provided nicknames (first + last name)
     * Unmatched names are treated as ghost members
     */
    private void fetchUIDsForNicknames(Set<String> nicknames, OnUIDsFetchedListener callback) {
        List<String> uids = new ArrayList<>();
        List<String> ghostMembers = new ArrayList<>();
        int total = nicknames.size();
        int[] done = {0};

        if (total == 0) {
            callback.onFetched(new UIDFetchResult(uids, ghostMembers));
            return;
        }

        // Split the nickname to get first and last name
        for (String name : nicknames) {
            String[] parts = name.trim().split("\\s+"); // matches white space characters
            // all users will have to key in first name and last name when registering, so if the nickname
            // doesn't contain both a first and last name, it is a ghost member. Add it to ghostMembers list
            if (parts.length < 2) {
                ghostMembers.add(name);
                if (++done[0] == total) callback.onFetched(new UIDFetchResult(uids, ghostMembers));
                continue;
            }

            String firstName = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
            String lastName = parts[parts.length - 1];

            // queries Firestore for users whose first_name and last_name match the split name parts
            db.collection("users")
                    .whereEqualTo("first_name", firstName)
                    .whereEqualTo("last_name", lastName)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                uids.add(doc.getId());
                            }
                        } else {
                            ghostMembers.add(name); // add the nickname to the ghostMembers list
                        }

                        if (++done[0] == total)
                            callback.onFetched(new UIDFetchResult(uids, ghostMembers));
                    })
                    .addOnFailureListener(e -> {
                        ghostMembers.add(name);
                        if (++done[0] == total)
                            callback.onFetched(new UIDFetchResult(uids, ghostMembers));
                    });
        }
    }

    /**
     * Method to fetch user by nickname (or display name)
     */
    public void fetchUserByNickname(String nickname, OnUserFetchListener listener) {
        // First check if we already have this user in our cache
        Map<String, User> currentUsers = usersMap.getValue();
        if (currentUsers != null && currentUsers.containsKey(nickname)) {
            listener.onUserFetched(currentUsers.get(nickname));
            return;
        }

        // Split the nickname to get first and last name
        String[] parts = nickname.trim().split("\\s+");

        if (parts.length < 2) {
            // Handle ghost members or single-name users
            User ghostUser = createPlaceholderUser(nickname);
            listener.onUserFetched(ghostUser);
            return;
        }

        String firstName = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
        String lastName = parts[parts.length - 1];

        // Query Firestore for this user
        db.collection("users")
                .whereEqualTo("first_name", firstName)
                .whereEqualTo("last_name", lastName)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                        // Extract user fields directly
                        String userId = document.getId();
                        String icon = document.getString("icon");
                        String split = "1"; // Default split value if needed

                        // Create user object with extracted data
                        User user = new User(userId, firstName, lastName, split, icon);

                        // Cache the user
                        Map<String, User> updatedUsers = new HashMap<>(currentUsers != null ? currentUsers : new HashMap<>());
                        updatedUsers.put(nickname, user);
                        usersMap.setValue(updatedUsers);

                        listener.onUserFetched(user);
                    } else {
                        // Create a placeholder user if no matching user found
                        User placeholderUser = createPlaceholderUser(nickname);
                        listener.onUserFetched(placeholderUser);
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to fetch user: " + e.getMessage());
                    User placeholderUser = createPlaceholderUser(nickname);
                    listener.onUserFetched(placeholderUser);
                });
    }

    /**
     * Create a placeholder user when we can't get real data
     */
    private User createPlaceholderUser(String nickname) {
         String firstName = nickname;
        String lastName = "";

        String[] parts = nickname.trim().split("\\s+");
        if (parts.length >= 2) {
            firstName = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
            lastName = parts[parts.length - 1];
        }

        return new User("ghost-" + UUID.randomUUID().toString(), firstName, lastName, "0.00", "ghostbeige");
    }

    /**
     * Interface for user fetch callbacks
     */
    public interface OnUserFetchListener {
        void onUserFetched(User user);
    }

    /**
     * Callback interface for retrieving UID.
     */
    private interface OnUIDsFetchedListener {
        void onFetched(UIDFetchResult result);
    }

    private static class UIDFetchResult {
        final List<String> uids;
        final List<String> ghostMembers;

        UIDFetchResult(List<String> uids, List<String> ghostMembers) {
            this.uids = uids;
            this.ghostMembers = ghostMembers;
        }
    }

    /**
     * Callback interface to notify when a group has been successfully created
     */
    public interface OnGroupCreatedListener {
        void onGroupCreated();
    }

    public void setOnGroupCreatedListener(OnGroupCreatedListener listener) {
        this.groupCreatedListener = listener;
    }

}
