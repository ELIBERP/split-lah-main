package com.example.split_lah.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.split_lah.R;
import com.example.split_lah.databinding.FragmentSettingsBinding;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.members.MembersViewModel;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private MembersViewModel membersViewModel;
    private SharedViewModel sharedViewModel;
    private String currentUserId;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Get activity-scoped ViewModels
        membersViewModel = new ViewModelProvider(requireActivity()).get(MembersViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize current user
        initializeCurrentUser();

        // Setup members section
        setupMembersSection();

        // Configure notification switch
        setupNotificationSwitch();

        // Setup group name display
        setupGroupName();

        setupFriendsList();

        setupUserAccount();

        return root;
    }

    private void initializeCurrentUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            membersViewModel.setCurrentUserId(currentUserId);
        }
    }

    private void setupGroupName() {
        // Observe group name changes
        sharedViewModel.getGroupName().observe(getViewLifecycleOwner(), groupName -> {
            if (groupName != null && !groupName.isEmpty()) {
                binding.tvGroupNameSettings.setText(groupName);
            } else {
                binding.tvGroupNameSettings.setText("No group selected");
            }
        });

        // Observe currency changes
        sharedViewModel.getCurrencyCode().observe(getViewLifecycleOwner(), currencyCode -> {
            if (currencyCode != null && !currencyCode.isEmpty()) {
                binding.tvSettingsCurrency.setText(currencyCode);
            } else {
                binding.tvSettingsCurrency.setText("SGD"); // Default currency if none set
            }
        });
    }

    private void setupMembersSection() {
        // Reference member image views
        ShapeableImageView member1 = binding.imgSettingsMember1;
        ShapeableImageView member2 = binding.imgSettingsMember2;
        ShapeableImageView member3 = binding.imgSettingsMember3;
        ShapeableImageView member4 = binding.imgSettingsMember4;
        ShapeableImageView overlayImage = binding.imgSettingsOverlay;
        TextView overlayText = binding.tvSettingsOverlay;

        // Initialize member views to invisible
        member1.setVisibility(View.GONE);
        member2.setVisibility(View.GONE);
        member3.setVisibility(View.GONE);
        member4.setVisibility(View.GONE);
        overlayImage.setVisibility(View.GONE);
        overlayText.setVisibility(View.GONE);

        // Add click listener to navigate to Members fragment
        binding.settingsMembers.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_members);
        });

        // Observe group ID changes
        sharedViewModel.getGroupId().observe(getViewLifecycleOwner(), groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                membersViewModel.setCurrentGroupId(groupId);
            }
        });

        // Observe the members list from MembersViewModel
        membersViewModel.getItems().observe(getViewLifecycleOwner(), membersList -> {
            if (membersList == null || membersList.isEmpty()) {
                return;
            }

            updateMemberIconsUI(membersList, membersList.size());
        });
    }

    private void updateMemberIconsUI(List<com.example.split_lah.ui.members.Item> membersList, int totalMemberCount) {
        // Get references to member image views
        ShapeableImageView member1 = binding.imgSettingsMember1;
        ShapeableImageView member2 = binding.imgSettingsMember2;
        ShapeableImageView member3 = binding.imgSettingsMember3;
        ShapeableImageView member4 = binding.imgSettingsMember4;
        ShapeableImageView overlayImage = binding.imgSettingsOverlay;
        TextView overlayText = binding.tvSettingsOverlay;

        // Reset visibility
        member1.setVisibility(View.GONE);
        member2.setVisibility(View.GONE);
        member3.setVisibility(View.GONE);
        member4.setVisibility(View.GONE);
        overlayImage.setVisibility(View.GONE);
        overlayText.setVisibility(View.GONE);

        // Display up to 4 members
        int displayCount = Math.min(membersList.size(), 4);

        if (displayCount > 0) {
            member1.setVisibility(View.VISIBLE);
            member1.setImageResource(IconUtils.getIconResourceId(membersList.get(0).getIcon()));
        }

        if (displayCount > 1) {
            member2.setVisibility(View.VISIBLE);
            member2.setImageResource(IconUtils.getIconResourceId(membersList.get(1).getIcon()));
        }

        if (displayCount > 2) {
            member3.setVisibility(View.VISIBLE);
            member3.setImageResource(IconUtils.getIconResourceId(membersList.get(2).getIcon()));
        }

        if (displayCount > 3) {
            member4.setVisibility(View.VISIBLE);
            member4.setImageResource(IconUtils.getIconResourceId(membersList.get(3).getIcon()));

            // If there are more than 4 other members (plus current user), show overlay
            // Remember totalMemberCount includes the current user
            if (totalMemberCount > 4) {
                int extraMembers = totalMemberCount - 4;
                overlayImage.setVisibility(View.VISIBLE);
                overlayText.setVisibility(View.VISIBLE);
                overlayText.setText("+" + extraMembers);
            }
        }
    }

    private void setupNotificationSwitch() {
        SwitchMaterial notificationSwitch = binding.switchPushNotification;

        boolean notificationsEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        notificationSwitch.setChecked(notificationsEnabled);

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                    Toast.makeText(requireContext(), "Please enable notifications in settings", Toast.LENGTH_SHORT).show();
                    goToNotificationSettings(null, requireContext());
                } else {
                    Toast.makeText(requireContext(), "Notifications already enabled", Toast.LENGTH_SHORT).show();
                }
            } else {
                goToNotificationSettings(null, requireContext());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void goToNotificationSettings(@Nullable String channel, Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (channel != null) {
                intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
            } else {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            }
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channel != null) {
                intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
            } else {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            }
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }
        context.startActivity(intent);
    }

    private void setupFriendsList() {
        // Reference the friends image views
        ShapeableImageView friendsIcon1 = binding.imgSettingsFriendsMember1;
        ShapeableImageView friendsIcon2 = binding.imgSettingsFriendsMember2;
        ShapeableImageView friendsIcon3 = binding.imgSettingsFriendsMember3;
        ShapeableImageView friendsIcon4 = binding.imgSettingsFriendsMember4;
        ShapeableImageView friendsOverlayImage = binding.imgSettingsFriendsOverlay;
        TextView friendsOverlayText = binding.tvSettingsFriendsOverlay;

        // Initialize friends views to invisible
        friendsIcon1.setVisibility(View.GONE);
        friendsIcon2.setVisibility(View.GONE);
        friendsIcon3.setVisibility(View.GONE);
        friendsIcon4.setVisibility(View.GONE);
        friendsOverlayImage.setVisibility(View.GONE);
        friendsOverlayText.setVisibility(View.GONE);

        // Observe BOTH the friends list and the member cache
        sharedViewModel.getFriendsList().observe(getViewLifecycleOwner(), friendIds -> {
            if (friendIds == null || friendIds.isEmpty()) {
                return; // No friends to display
            }

            // Check if any friend data is already in the member cache
            Map<String, SharedViewModel.UserData> cache = sharedViewModel.getMemberCache().getValue();
            List<FriendData> friendDataList = new ArrayList<>();
            List<String> uncachedFriendIds = new ArrayList<>();

            // Use cached data where available
            for (String friendId : friendIds) {
                if (cache != null && cache.containsKey(friendId)) {
                    SharedViewModel.UserData userData = cache.get(friendId);
                    friendDataList.add(new FriendData(
                            friendId,
                            userData.getFirstName(),
                            userData.getIcon()
                    ));
                } else {
                    uncachedFriendIds.add(friendId);
                }
            }

            // Only fetch uncached user data
            if (!uncachedFriendIds.isEmpty()) {
                fetchUncachedFriendsData(uncachedFriendIds, friendDataList, friendIds.size());
            } else {
                // All data was cached
                updateFriendsIconsUI(friendDataList, friendIds.size());
            }
        });
    }

    // Only fetch data for friends not in cache
    private void fetchUncachedFriendsData(List<String> uncachedFriendIds,
                                          List<FriendData> existingFriendData,
                                          int totalFriends) {
        final int[] processedCount = {0};

        for (String friendId : uncachedFriendIds) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(friendId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String firstName = task.getResult().getString("first_name");
                            String icon = task.getResult().getString("icon");
                            String lastName = task.getResult().getString("last_name");

                            // Add to our result list
                            existingFriendData.add(new FriendData(friendId, firstName, icon));

                            // Also add to SharedViewModel cache for future use
                            sharedViewModel.updateMemberCache(friendId, new SharedViewModel.UserData(
                                    friendId, firstName, lastName, icon, "Friend"
                            ));
                        }

                        processedCount[0]++;
                        if (processedCount[0] >= uncachedFriendIds.size()) {
                            // All uncached friends processed, update UI
                            updateFriendsIconsUI(existingFriendData, totalFriends);
                        }
                    });
        }
    }

    private void fetchFriendsData(List<String> friendIds) {
        List<FriendData> friendDataList = new ArrayList<>();
        final int[] processedCount = {0};
        final int totalFriends = friendIds.size();

        if (totalFriends == 0) {
            updateFriendsIconsUI(new ArrayList<>(), 0);
            return;
        }

        for (String friendId : friendIds) {
            // Fetch each friend's data from Firestore
            FirebaseFirestore.getInstance().collection("users")
                    .document(friendId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String firstName = task.getResult().getString("first_name");
                            String icon = task.getResult().getString("icon");

                            friendDataList.add(new FriendData(friendId, firstName, icon));
                        }

                        processedCount[0]++;
                        if (processedCount[0] >= totalFriends) {
                            // All friends processed, update UI
                            updateFriendsIconsUI(friendDataList, friendDataList.size());
                        }
                    })
                    .addOnFailureListener(e -> {
                        processedCount[0]++;
                        if (processedCount[0] >= totalFriends) {
                            updateFriendsIconsUI(friendDataList, friendDataList.size());
                        }
                    });
        }
    }

    private void updateFriendsIconsUI(List<FriendData> friendsList, int totalFriendsCount) {
        if (binding == null) return; // Fragment view might be destroyed

        ShapeableImageView friendsIcon1 = binding.imgSettingsFriendsMember1;
        ShapeableImageView friendsIcon2 = binding.imgSettingsFriendsMember2;
        ShapeableImageView friendsIcon3 = binding.imgSettingsFriendsMember3;
        ShapeableImageView friendsIcon4 = binding.imgSettingsFriendsMember4;
        ShapeableImageView friendsOverlayImage = binding.imgSettingsFriendsOverlay;
        TextView friendsOverlayText = binding.tvSettingsFriendsOverlay;

        // Reset visibility
        friendsIcon1.setVisibility(View.GONE);
        friendsIcon2.setVisibility(View.GONE);
        friendsIcon3.setVisibility(View.GONE);
        friendsIcon4.setVisibility(View.GONE);
        friendsOverlayImage.setVisibility(View.GONE);
        friendsOverlayText.setVisibility(View.GONE);

        // Display up to 4 friends
        int displayCount = Math.min(friendsList.size(), 4);

        if (displayCount > 0) {
            friendsIcon1.setVisibility(View.VISIBLE);
            friendsIcon1.setImageResource(IconUtils.getIconResourceId(friendsList.get(0).icon));
        }

        if (displayCount > 1) {
            friendsIcon2.setVisibility(View.VISIBLE);
            friendsIcon2.setImageResource(IconUtils.getIconResourceId(friendsList.get(1).icon));
        }

        if (displayCount > 2) {
            friendsIcon3.setVisibility(View.VISIBLE);
            friendsIcon3.setImageResource(IconUtils.getIconResourceId(friendsList.get(2).icon));
        }

        if (displayCount > 3) {
            friendsIcon4.setVisibility(View.VISIBLE);
            friendsIcon4.setImageResource(IconUtils.getIconResourceId(friendsList.get(3).icon));

            // If there are more friends than we can display, show the overlay
            if (totalFriendsCount > 4) {
                int extraFriends = totalFriendsCount - 4;
                friendsOverlayImage.setVisibility(View.VISIBLE);
                friendsOverlayText.setVisibility(View.VISIBLE);
                friendsOverlayText.setText("+" + extraFriends);
            }
        }
    }

    // Helper class to store friend data
    private static class FriendData {
        final String id;
        final String name;
        final String icon;

        FriendData(String id, String name, String icon) {
            this.id = id;
            this.name = name != null ? name : "Friend";
            this.icon = icon != null ? icon : "giraffe"; // Default icon
        }
    }

    private void setupUserAccount() {
        // Observe user's first name for UI
        sharedViewModel.getUserFirstName().observe(getViewLifecycleOwner(), firstName -> {
            if (firstName != null && !firstName.isEmpty()) {
                binding.tvSettingsAccountName.setText(firstName);
            } else {
                binding.tvSettingsAccountName.setText("User");
            }
        });
    }


}


