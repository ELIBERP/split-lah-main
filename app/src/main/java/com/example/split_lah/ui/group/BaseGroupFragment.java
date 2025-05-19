package com.example.split_lah.ui.group;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.navigation.Navigation;

import com.example.split_lah.MainActivity;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.models.User;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.friends.FriendsBottomSheet;
import com.example.split_lah.ui.newpermgrp.NewPermanentGroupBottomSheet;
import com.example.split_lah.ui.split.SplitBillBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import com.example.split_lah.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This fragment is a base for "Temporary Group" and "Permanent Group" creation pages
 */
public class BaseGroupFragment extends Fragment implements FriendsBottomSheet.OnFriendsSelectedListener {

    // UI elements
    protected TextInputEditText groupNameEditText;
    protected MaterialButton addMembersButton, addBillButton, addGroupButton;
    protected AutoCompleteTextView currencySpinner;
    protected LinearLayout otherMembersContainer;
    protected TextView nicknameTextView;
    protected NestedScrollView membersScrollView;
    protected BaseGroupViewModel viewModel;
    private String groupType;

    private String groupId;
    private ImageView userIconImageView;

    // Add this field to your BaseGroupFragment class
    private final ArrayList<User> ghostMembersList = new ArrayList<>();


    // tracks added members to avoid duplications
    private final Set<String> addedMembers = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // the group_type is passed in automatically via the <argument> in the mobile_navigation.xml
        if (getArguments() != null) {
            groupType = getArguments().getString("group_type", "tempgrp");
        }
        return inflater.inflate(R.layout.fragment_new_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize views and set up listeners
        initializeViews(view);
        setupViewModel();
        updateGroupTitle(view);  // update title ("Add Group" or "Add Temporary Group") depending on which page user is in
        toggleButtonVisibility(view);  // show relevant buttons depending on which page user is in
        setupListeners();
        observeViewModel();
        checkAddBillButtonState();
    }

    private void observeViewModel() {
        // observe new group ID from the BaseGroupViewModel
        viewModel.getNewGroupId().observe(getViewLifecycleOwner(), groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                Toast.makeText(requireContext(), "Group created successfully!", Toast.LENGTH_SHORT).show();
                // Navigate to the newly created group
                navigateToNewlyCreatedGroup(groupId);
                resetForm();
            }
        });
        // observe any error msgs and display them in a toast
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // resets the form input fields after clicking Add Group button
    private void resetForm() {

        groupNameEditText.setText(""); // clear group name
        if (!Objects.requireNonNull(viewModel.getAvailableCurrencies().getValue()).isEmpty()) {
            currencySpinner.setText(viewModel.getAvailableCurrencies().getValue().get(0), false);
        }
        otherMembersContainer.removeAllViews(); // clear added members
        addedMembers.clear(); // reset added members hashset
    }

    // navigate to the newly created group's overview page
    private void navigateToNewlyCreatedGroup(String groupId) {
        // get the group name
        String groupName = Objects.requireNonNull(groupNameEditText.getText()).toString().trim();

        // update SharedViewModel with the new group info
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.setGroupId(groupId);
        sharedViewModel.setGroupName(groupName);

        // navigate to the HomeFragment
        if (isAdded() && getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.nav_home);
        }
    }

    private void initializeViews(View view) {
        // bind UI components
        groupNameEditText = view.findViewById(R.id.groupNameEditText);
        addMembersButton = view.findViewById(R.id.addMembersButton);
        currencySpinner = view.findViewById(R.id.currency_spinner);
        addBillButton = view.findViewById(R.id.addBillButton);
        addGroupButton = view.findViewById(R.id.addGroupButton);
        otherMembersContainer = view.findViewById(R.id.otherMembersContainer);
        membersScrollView = view.findViewById(R.id.membersScrollView);
        nicknameTextView = view.findViewById(R.id.nicknameTextView);
        userIconImageView = view.findViewById(R.id.userIconImageView);

    }

    private void setupViewModel() {
        // initialize ViewModel and observe relevant data
        viewModel = new ViewModelProvider(this).get(BaseGroupViewModel.class);
        viewModel.getGroupName().observe(getViewLifecycleOwner(), name -> {
            if (!name.equals(Objects.requireNonNull(groupNameEditText.getText()).toString())) {
                groupNameEditText.setText(name);
            }
        });
        viewModel.getAvailableCurrencies().observe(getViewLifecycleOwner(), this::populateCurrencySpinner);
        viewModel.getFullName().observe(getViewLifecycleOwner(), nicknameTextView::setText);
        viewModel.getUserIcon().observe(getViewLifecycleOwner(), iconResourceId -> {
            if (iconResourceId != null) {
                userIconImageView.setImageResource(iconResourceId);
            }
        });

        viewModel.loadCurrenciesFromFirestore();
        viewModel.getLoggedInUserFullName();
        // once a group is successfully created, tell the MainActivity to update the side navbar
        viewModel.setOnGroupCreatedListener(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).fetchUserData();
            }
        });
    }

    private void setupListeners() {
        groupNameEditText.addTextChangedListener(new SimpleTextWatcher(s -> {
            String newValue = s.toString();
            if (!newValue.equals(viewModel.getGroupName().getValue())) {
                viewModel.setGroupName(newValue);
            }
            checkAddBillButtonState();
        }));

        addMembersButton.setOnClickListener(v -> {
            if ("permgrp".equals(groupType)) {
                openNewPermanentGroupBottomSheet();
            } else {
                openAddMembersBottomSheet();
            }
            checkAddBillButtonState();
        });

        addBillButton.setOnClickListener(v -> {
            // only open the SplitBillBottomSheet when user is in New Temporary Group page
            if ("tempgrp".equals(groupType)) {
                String groupName = Objects.requireNonNull(groupNameEditText.getText()).toString().trim();
                String currency = currencySpinner.getText().toString().trim();

                // validation
                if (groupName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a group name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (addedMembers.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add at least one member", Toast.LENGTH_SHORT).show();
                    return;
                }
                openSplitBillBottomSheet();
            }
        });

        addGroupButton.setOnClickListener(v -> {
            if ("permgrp".equals(groupType)) {
                String groupName = Objects.requireNonNull(groupNameEditText.getText()).toString().trim();
                String currency = currencySpinner.getText().toString().trim();

                // validation
                if (groupName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a group name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (addedMembers.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add at least one member", Toast.LENGTH_SHORT).show();
                    return;
                }

                // save the permanent group using the ViewModel
                viewModel.savePermanentGroup(groupName, currency, addedMembers);
            }
        });
    }

    /**
     * Display correct buttons based on group type
     */
    private void toggleButtonVisibility(View view) {
        // show either "Add Group" or "Add Bill" button depending on the group type
        int buttonView = "permgrp".equals(groupType) ? R.id.addGroupButton : R.id.addBillButton;
        view.findViewById(buttonView).setVisibility(View.VISIBLE);

        // adjust button placement
        ConstraintLayout mainLayout = (ConstraintLayout) view;
        ConstraintSet set = new ConstraintSet();
        set.clone(mainLayout);

        set.connect(R.id.membersCard, ConstraintSet.BOTTOM, buttonView, ConstraintSet.TOP, dpToPx(16));
        set.applyTo(mainLayout);
        addMembersButton.setVisibility(View.VISIBLE);
    }

    /**
     * Only enable Add Bill button when Group Name has input and at least one other member is added
     */
    private void checkAddBillButtonState() {
        // Disable Add Bill button if no group name or no members added
        boolean isGroupNameEmpty = Objects.requireNonNull(groupNameEditText.getText()).toString().trim().isEmpty();
        boolean areNoMembersAdded = addedMembers.isEmpty();

        if ("tempgrp".equals(groupType)) {
            addBillButton.setEnabled(!isGroupNameEmpty && !areNoMembersAdded);
        }
    }

    /**
     * Display correct title based on group type
     */
    private void updateGroupTitle(View view) {
        int titleViewId = "permgrp".equals(groupType) ? R.id.permGroupTitleTextView : R.id.tempGroupTitleTextView;
        view.findViewById(titleViewId).setVisibility(View.VISIBLE);
        updateTitleViewConstraints(view, titleViewId);
    }

    /**
     * Populate currency dropdown
     */
    private void populateCurrencySpinner(List<String> currencies) {
        currencySpinner.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, currencies));
        if (!currencies.isEmpty()) currencySpinner.setText(currencies.get(0), false);
    }

    /**
     * Show bottom sheet for adding ghost members (temp group)
     */
    private void openAddMembersBottomSheet() {
        NewGhostMemberBottomSheet bottomSheet = new NewGhostMemberBottomSheet();
        bottomSheet.setOnNicknameAddedListener(this::addMemberToUI);
        bottomSheet.setGroupType(groupType);
        bottomSheet.show(getParentFragmentManager(), "NewGhostMemberBottomSheet");
    }

    /**
     * Show bottom sheet for adding existing friends or ghost members (perm group)
     */
    private void openNewPermanentGroupBottomSheet() {
        NewPermanentGroupBottomSheet bottomSheet = new NewPermanentGroupBottomSheet();
        bottomSheet.setParentNicknameListener(this::addMemberToUI);
        bottomSheet.show(getParentFragmentManager(), "NewPermanentGroupBottomSheet");
    }

    /**
     * Show bottom sheet for adding a new bill
     */
    private void openSplitBillBottomSheet() {
        ArrayList<User> ghostMembers = getGhostMembers();
        SplitBillBottomSheet bottomSheet = new SplitBillBottomSheet(groupId, ghostMembers);
        bottomSheet.show(getParentFragmentManager(), "SplitBillBottomSheet");
    }

    @Override
    public void onFriendsSelected(Map<String, String> selectedFriends) {
        List<String> duplicates = new ArrayList<>();

        // add selected friends to the UI and check for duplicates
        selectedFriends.values().forEach(displayName -> {
            if (addedMembers.contains(displayName)) {
                duplicates.add(displayName);
            } else {
                addMemberToUI(displayName);
            }
        });

        // show toast message if there are duplicates
        if (!duplicates.isEmpty()) {
            String message = (duplicates.size() == 1)
                    ? duplicates.get(0) + " is already added to the group!"
                    : "They have already been added to the group!";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Interface used to notify parent that a nickname was added
     */
    public interface OnNicknameAddedListener {
        void onNicknameAdded(String nickname);
    }

    /**
     * Retrieves user data by nickname either from cache or Firestore
     */
    private void getUserByNickname(String nickname, BaseGroupViewModel.OnUserFetchListener listener) {
        viewModel.fetchUserByNickname(nickname, listener);
    }

    /**
     * Adds a member to the UI if not already added
     */
    public void addMemberToUI(String nickname) {
        // prevent adding a same member twice
        if (addedMembers.contains(nickname)) {
            Toast.makeText(requireContext(), nickname + " is already in this group.", Toast.LENGTH_SHORT).show();
            return;
        }

        addedMembers.add(nickname);

        // Fetch user data from Firestore
        getUserByNickname(nickname, user -> {
            // Add the user to our ghost members list
            ghostMembersList.add(user);

            View memberView = createMemberLayout(user);
            otherMembersContainer.addView(memberView);

            updateAddMemberButtonConstraints();
            checkAddBillButtonState();
        });
    }

    /**
     * Builds the horizontal layout for each member
     */
    private LinearLayout createMemberLayout(User user) {
        LinearLayout memberLayout = new LinearLayout(requireContext());
        memberLayout.setOrientation(LinearLayout.HORIZONTAL);
        memberLayout.setGravity(Gravity.CENTER_VERTICAL);
        memberLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, dpToPx(4), 0, 0);
        memberLayout.setLayoutParams(layoutParams);

        // Set background
        memberLayout.setBackgroundResource(R.drawable.member_item_background);

        // Create and add the ImageView for the user's icon
        ImageView iconImageView = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
        iconParams.setMarginEnd(dpToPx(12));
        iconImageView.setLayoutParams(iconParams);

        iconImageView.setImageResource(IconUtils.getIconResourceId(user.getIcon()));
        memberLayout.addView(iconImageView);

        // Create and add the TextView for the user's name
        TextView memberTextView = new TextView(requireContext());
        // Construct the display name based on first and last name
        String displayName = user.getFirstName();
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            displayName += " " + user.getLastName();
        }
        memberTextView.setText(displayName);
        memberTextView.setTextSize(16);
        memberTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.weight = 1;
        memberTextView.setLayoutParams(textParams);
        memberLayout.addView(memberTextView);

        // cancel button to remove member
        ImageView deleteButton = new ImageView(requireContext());
        deleteButton.setImageResource(R.drawable.ic_close);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        deleteButton.setLayoutParams(deleteParams);
        // In the createMemberLayout method, update the deleteButton.setOnClickListener:
        deleteButton.setOnClickListener(v -> {
            otherMembersContainer.removeView(memberLayout);
            // remove from addedMembers set
            String memberName = user.getFirstName();
            if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                memberName += " " + user.getLastName();
            }
            addedMembers.remove(memberName);

            // Also remove from ghost members list
            ghostMembersList.remove(user);

            checkAddBillButtonState();
        });
        memberLayout.addView(deleteButton);

        return memberLayout;
    }

    /**
     * Update constraints after a member is added
     */
    private void updateAddMemberButtonConstraints() {
        ConstraintLayout layout = requireView().findViewById(R.id.membersCardConstraintLayout);
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);

        addMembersButton.setVisibility(View.VISIBLE);

        set.connect(R.id.addMembersButton, ConstraintSet.TOP, R.id.membersScrollView, ConstraintSet.BOTTOM, dpToPx(16));
        set.connect(R.id.addMembersButton, ConstraintSet.TOP, R.id.membersScrollView, ConstraintSet.BOTTOM, dpToPx(16));
        set.connect(R.id.addMembersButton, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(16));

        set.applyTo(layout);
    }

    private void updateTitleViewConstraints(View view, int titleViewId) {
        MaterialCardView groupDetailsCard = view.findViewById(R.id.groupDetailsCard);
        ((ConstraintLayout.LayoutParams) groupDetailsCard.getLayoutParams()).topToBottom = titleViewId;
    }

    private ArrayList<User> getGhostMembers() {
        return new ArrayList<>(ghostMembersList);
    }

    /**
     * Helper method to convert dp to pixels
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final TextUpdateListener listener;

        SimpleTextWatcher(TextUpdateListener listener) {
            this.listener = listener;
        }

        @Override
        public void afterTextChanged(Editable s) {
            listener.onTextChanged(s);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * Interface for responding to text changes
     */
    private interface TextUpdateListener {
        void onTextChanged(Editable s);
    }
}