package com.example.split_lah.ui.members;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MembersViewModel extends ViewModel {

    private static final String TAG = "MembersViewModel";
    private final FirebaseFirestore db;
    private String currentGroupId = null;
    private final MutableLiveData<List<Item>> mItems;
    private String currentUserId;

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public MembersViewModel() {
        db = FirebaseFirestore.getInstance();
        mItems = new MutableLiveData<>();
    }

    public void setCurrentGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Log.w(TAG, "Ignoring empty group ID");
            return;
        }

        this.currentGroupId = groupId;
        Log.d(TAG, "Group ID changed to: " + groupId);
        fetchItems();
    }

    public LiveData<List<Item>> getItems() {
        return mItems;
    }


    private void fetchItems() {
        if (currentGroupId == null || currentGroupId.isEmpty()) {
            Log.w(TAG, "Cannot fetch items: No group ID set");
            mItems.setValue(new ArrayList<>());
            return;
        }

        List<Item> items = new ArrayList<>();

        db.collection("permanent_grp")
                .document(currentGroupId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        if (!document.exists()) {
                            Log.w(TAG, "Group document doesn't exist");
                            mItems.setValue(new ArrayList<>());
                            return;
                        }

                        // Get members list (expecting strings, not longs)
                        List<String> members = (List<String>) document.get("members");
                        List<String> ghostMembers = (List<String>) document.get("ghost_members");
                        String owner = document.getString("owner");

                        if (members.isEmpty() && ghostMembers.isEmpty()) {
                            Log.d(TAG, "No members found in group");
                            mItems.setValue(new ArrayList<>());
                            return;
                        }

                        Log.d(TAG, "Found " + members.size() + " regular members and " +
                                ghostMembers.size() + " ghost members in group");

                        db.collection("users")
                                .whereIn(FieldPath.documentId(), members)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        items.clear();

                                        for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                            String userId = document2.getId();
                                            String firstName = document2.getString("first_name");
                                            String lastName = document2.getString("last_name");
                                            String icon = document2.getString("icon");

                                            String role = userId.equals(owner) ? "Owner" : "Member";

                                            // Add user to items list with icon
                                            items.add(new Item(userId, firstName, lastName, role, icon));
                                        }
                                        for (String ghostName : ghostMembers) {
                                            // Ghost members have name but no ID, last name or icon
                                            items.add(new Item("", ghostName, "", "Ghost", "giraffe"));
                                        }
                                        sortMembersByRole(items);
                                        mItems.setValue(items);
                                        Log.d(TAG, "Successfully loaded " + items.size() + " members");

                                    } else {
                                        Log.e(TAG, "Error getting user documents", task2.getException());
                                        mItems.setValue(new ArrayList<>());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to fetch user data", e);
                                    items.clear();
                                    for (String ghostName : ghostMembers) {
                                        items.add(new Item("", ghostName, "", "Ghost", "ghost"));
                                    }
                                    mItems.setValue(items);
                                });
                    } else {
                        Log.e(TAG, "Error getting group document", task.getException());
                        mItems.setValue(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch group data", e);
                    mItems.setValue(new ArrayList<>());
                });
    }

    private void sortMembersByRole(List<Item> items) {
        items.sort((item1, item2) -> {
            // Define role priorities using a function
            java.util.function.Function<String, Integer> getPriority = role -> {
                switch (role) {
                    case "Owner": return 0;      // Highest priority
                    case "Member": return 1;     // Medium priority
                    case "Ghost": return 2; // Lowest priority
                    default: return 3;           // Unknown roles go last
                }
            };

            int priority1 = getPriority.apply(item1.getRole());
            int priority2 = getPriority.apply(item2.getRole());

            // Sort by role priority first
            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }

            // If same role, sort by name for consistency
            return item1.getFirstName().compareToIgnoreCase(item2.getFirstName());
        });

        Log.d(TAG, "Sorted " + items.size() + " members by role priority");
    }

    @Override
    protected void onCleared() {
        super.onCleared();

    }

    public void forceRefresh() {
        if (currentGroupId != null && !currentGroupId.isEmpty()) {
            fetchItems();
        }
    }
}