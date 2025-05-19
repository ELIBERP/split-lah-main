package com.example.split_lah.ui.friends;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FriendsViewModel extends ViewModel {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private MutableLiveData<Map<String, String>> friendsListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    // Fetch group members and user names
    public void fetchGroupAndUserNames(String userId) {
        List<String> allMembers = new ArrayList<>();

        db.collection("permanent_grp")
                .whereArrayContains("members", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (var groupDoc : task.getResult()) {
                            List<String> members = (List<String>) groupDoc.get("members");

                            if (members != null) {
                                members.remove(userId); // Remove logged-in user from the list
                                allMembers.addAll(members);
                            }
                        }

                        // Ensure unique members
                        List<String> uniqueMembers = new ArrayList<>(new HashSet<>(allMembers));

                        // Fetch usernames for the members
                        fetchUserNamesForMembers(uniqueMembers);
                    } else {
                        errorMessageLiveData.setValue("Error fetching group data.");
                    }
                });
    }

    private void fetchUserNamesForMembers(List<String> userIds) {
        Map<String, String> userNames = new HashMap<>();
        final int[] completedRequests = {0};
        int totalUsers = userIds.size();

        // Fetch each user's first name from the users collection
        for (String uid : userIds) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get first_name and last_name fields
                            String firstName = documentSnapshot.getString("first_name");
                            String lastName = documentSnapshot.getString("last_name");

                            // Combine first and last name
                            String fullName = firstName;
                            if (lastName != null && !lastName.isEmpty()) {
                                fullName += " " + lastName;
                            }

                            userNames.put(uid, fullName);
                        }

                        completedRequests[0]++;

                        // Once all requests are completed, send data to the UI
                        if (completedRequests[0] >= totalUsers) {
                            friendsListLiveData.setValue(userNames); // Set the usernames in LiveData
                        }
                    });
        }
    }

    public LiveData<Map<String, String>> getFriendsList() {
        return friendsListLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }
}
