package com.example.split_lah.ui.friends;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.split_lah.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

/**
 * Bottom sheet that displays the user's friends list to allow the user to add them to a group
 */
public class FriendsBottomSheet extends BottomSheetDialogFragment {

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FriendsViewModel friendsViewModel;

    // stores currently selected friends
    private final Map<String, String> selectedFriends = new HashMap<>();
    private OnFriendsSelectedListener onFriendsSelectedListener;

    /**
     * Callback interface for returning selected friends back to BaseGroupFragment
     */
    public interface OnFriendsSelectedListener {
        void onFriendsSelected(Map<String, String> selectedFriends);
    }

    /**
     * Setter for listener to receive selected friends
     */
    public void setOnFriendsSelectedListener(OnFriendsSelectedListener listener) {
        this.onFriendsSelectedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_friends, container, false);

        // initialize the ViewModel
        friendsViewModel = new ViewModelProvider(requireActivity()).get(FriendsViewModel.class);

        // observe LiveData for friends list and update UI
        friendsViewModel.getFriendsList().observe(getViewLifecycleOwner(), userNames -> {
            if (userNames != null) {
                displayMembersInUI(view, userNames);
            }
        });

        friendsViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton cancelButton = view.findViewById(R.id.cancelButton);
        Button addFriendsToGroupButton = view.findViewById(R.id.addFriendsToGroupButton);

        cancelButton.setOnClickListener(v -> dismiss());
        addFriendsToGroupButton.setOnClickListener(v -> {
            // return selected friends back to the parent via listener
            if (onFriendsSelectedListener != null) {
                onFriendsSelectedListener.onFriendsSelected(new HashMap<>(selectedFriends));
            }
            dismiss();
        });

        // get current user ID and fetch friends list
        String userId = mAuth.getCurrentUser().getUid();
        if (userId != null && !userId.isEmpty()) {
            friendsViewModel.fetchGroupAndUserNames(userId);
        }

        return view;
    }

    /**
     * Dynamically creates and displays a list of friends with checkboxes
     */
    private void displayMembersInUI(View view, Map<String, String> userNames) {
        LinearLayout friendsListLayout = view.findViewById(R.id.friendsListLayout);
        friendsListLayout.removeAllViews(); // clear any previous entries

        for (Map.Entry<String, String> entry : userNames.entrySet()) {
            String friendId = entry.getKey();
            String displayName = entry.getValue();

            // create a new LinearLayout for each friend
            LinearLayout friendLayout = new LinearLayout(getContext());
            friendLayout.setOrientation(LinearLayout.HORIZONTAL);
            friendLayout.setGravity(Gravity.START);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 16);
            friendLayout.setLayoutParams(layoutParams);

            // TextView for displaying friend's name
            TextView friendTextView = new TextView(getContext());
            friendTextView.setText(displayName);
            friendTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
            ));
            friendTextView.setPadding(10, 8, 10, 8);
            friendTextView.setTextSize(16);

            // checkbox for selecting friend
            CheckBox friendCheckBox = new CheckBox(getContext());
            friendCheckBox.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            friendCheckBox.setTag(friendId);

            // handle checkbox selection and update selectedFriends map
            friendCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedFriends.put(friendId, displayName);
                } else {
                    selectedFriends.remove(friendId);
                }
            });

            // add name and checkbox to layout
            friendLayout.addView(friendTextView);
            friendLayout.addView(friendCheckBox);
            friendsListLayout.addView(friendLayout);
        }
    }

    /**
     * Ensures the bottom sheet opens and fills the entire screen height
     */
    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null) {
            View view = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (view != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}
