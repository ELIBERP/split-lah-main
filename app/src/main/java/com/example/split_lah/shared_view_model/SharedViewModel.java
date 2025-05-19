package com.example.split_lah.shared_view_model;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SharedViewModel holds and manages shared data related to a permanent group
 * so it can be accessed across different fragments and activities
 * It interacts with Firestore to fetch group information using LiveData
 */
public class SharedViewModel extends ViewModel {

    // LiveData to hold various group related data
    private final MutableLiveData<String> groupId = new MutableLiveData<>();
    private final MutableLiveData<String> groupName = new MutableLiveData<>();
    private final MutableLiveData<String> currencyCode = new MutableLiveData<>();
    private final MutableLiveData<List<String>> members = new MutableLiveData<>();
    private final MutableLiveData<List<String>> ghostMembers = new MutableLiveData<>();
    private final MutableLiveData<String> owner = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "SharedViewModel";
    private static final String COLLECTION_GROUPS = "permanent_grp";

    // User related data
    private final MutableLiveData<String> userFirstName = new MutableLiveData<>();
    private final MutableLiveData<String> userIcon = new MutableLiveData<>();

    // Member cache
    private final MutableLiveData<Map<String, UserData>> memberCache = new MutableLiveData<>(new HashMap<>());
    private long memberCacheLastUpdated = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Class to store member user data
     */
    public static class UserData {
        private final String userId;
        private final String firstName;
        private final String lastName;
        private final String icon;
        private final String role;

        public UserData(String userId, String firstName, String lastName, String icon, String role) {
            this.userId = userId;
            this.firstName = firstName != null ? firstName : "";
            this.lastName = lastName != null ? lastName : "";
            this.icon = icon;
            this.role = role;
        }

        public String getUserId() { return userId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getIcon() { return icon; }
        public String getRole() { return role; }
    }

    // Group data getters and setters
    public LiveData<String> getGroupId() {
        return groupId;
    }

    public void setGroupId(String id) {
        groupId.setValue(id);
        invalidateMemberCache(); // Clear cache when group changes
    }

    public LiveData<String> getGroupName() {
        return groupName;
    }

    public void setGroupName(String name) {
        groupName.setValue(name);
    }

    public LiveData<String> getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String code) {
        currencyCode.setValue(code);
    }

    // Getter and setter for members
    public MutableLiveData<List<String>> getMembers() {
        return members;
    }
    public void setMembers(List<String> membersList) {
        members.setValue(membersList);
    }

    // Getter and setter for ghostMembers
    public MutableLiveData<List<String>> getGhostMembers() {
        return ghostMembers;
    }

    public void setGhostMembers(List<String> ghostMembersList) {
        ghostMembers.setValue(ghostMembersList);
    }

    // Getter and setter for owner of the group
    public MutableLiveData<String> getOwner() {
        return owner;
    }

    public void setOwner(String ownerName) {
        owner.setValue(ownerName);
    }

    // User data getters and setters
    public LiveData<String> getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String firstName) {
        userFirstName.setValue(firstName);
    }

    public LiveData<String> getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(String icon) {
        userIcon.setValue(icon);
    }

    // Member cache methods
    public LiveData<Map<String, UserData>> getMemberCache() {
        return memberCache;
    }

    public boolean isMemberCacheValid() {
        Map<String, UserData> cache = memberCache.getValue();
        return cache != null && !cache.isEmpty() &&
                (System.currentTimeMillis() - memberCacheLastUpdated < CACHE_VALIDITY_MS);
    }

    public void updateMemberCache(String userId, UserData userData) {
        Map<String, UserData> cache = memberCache.getValue();
        if (cache != null) {
            cache.put(userId, userData);
            memberCache.setValue(cache);
            memberCacheLastUpdated = System.currentTimeMillis();
        }
    }

    public void updateMemberCache(Map<String, UserData> newData) {
        Map<String, UserData> cache = memberCache.getValue();
        if (cache != null) {
            cache.putAll(newData);
            memberCache.setValue(cache);
            memberCacheLastUpdated = System.currentTimeMillis();
        }
    }

    public void invalidateMemberCache() {
        memberCache.setValue(new HashMap<>());
        memberCacheLastUpdated = 0;
    }

    private final MutableLiveData<Boolean> fromNotification = new MutableLiveData<>(false);

    public void setFromNotification(boolean value) {
        fromNotification.setValue(value);
    }

    public LiveData<Boolean> getFromNotification() {
        return fromNotification;
    }

    /**
     * Fetch group data from Firestore based on groupId
     * @param groupId The group ID to fetch data for
     */
    public void fetchGroupData(String groupId) {
        db.collection(COLLECTION_GROUPS)
                .document(groupId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String currencyCode = document.contains("currency_code") ?
                                    document.getString("currency_code") : null;

                            List<String> members = (List<String>) document.get("members");
                            if (members == null) {
                                members = new ArrayList<>();
                            }

                            List<String> ghostMembers = (List<String>) document.get("ghost_members");
                            if (ghostMembers == null) {
                                ghostMembers = new ArrayList<>();
                            }

                            String owner = document.contains("owner") ?
                                    document.getString("owner") : null;

                            // update LiveData with fetched values
                            String groupName = document.contains("group_name") ?
                                    document.getString("group_name") : null;

                            // Update all values
                            setCurrencyCode(currencyCode);
                            setMembers(members);
                            setGhostMembers(ghostMembers);
                            setOwner(owner);
                            setGroupName(groupName);

                            Log.d(TAG, "Successfully fetched group data: " + groupName);
                        } else {
                            Log.d("Firestore", "No such document!");
                            Log.d(TAG, "No such document for group ID: " + groupId);
                        }
                    } else {
                        Log.d("Firestore", "Error getting document...", task.getException());
                        Log.e(TAG, "Error getting group document", task.getException());
                    }
                });
    }

    private final MutableLiveData<List<String>> friendsList = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<String>> getFriendsList() {
        return friendsList;
    }

    public void setFriendsList(List<String> friends) {
        friendsList.setValue(friends);
        Log.d(TAG, "Updated friends list with " + friends.size() + " friends");
    }
}