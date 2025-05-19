package com.example.split_lah.ui.newpermgrp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.split_lah.ui.friends.FriendsBottomSheet;
import com.example.split_lah.ui.group.BaseGroupFragment;
import com.example.split_lah.ui.group.NewGhostMemberBottomSheet;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.example.split_lah.R;

import java.util.Map;

/**
 * Bottom sheet for adding members to a new permanent group.
 * Users can either add ghost members (members without accounts) or existing friends.
 */
public class NewPermanentGroupBottomSheet extends BottomSheetDialogFragment {

    // UI elements
    private Button createGhostMemberButton;
    private Button addExistingMemberButton;
    private ImageButton cancelButton;

    // Listener to notify parent fragment when a nickname is added
    private BaseGroupFragment.OnNicknameAddedListener parentListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof BaseGroupFragment) {
            BaseGroupFragment baseGroupFragment = (BaseGroupFragment) parentFragment;
            parentListener = baseGroupFragment::addMemberToUI;
        }

        // initialize buttons from layout
        cancelButton = view.findViewById(R.id.cancelButton);
        createGhostMemberButton = view.findViewById(R.id.createGhostMemberButton);
        addExistingMemberButton = view.findViewById(R.id.addExistingMemberButton);

        // close bottom sheet on cancel
        cancelButton.setOnClickListener(v -> dismiss());

        // handle adding ghost member
        createGhostMemberButton.setOnClickListener(v -> {
            if (parentListener == null) {
                dismiss();
                return;
            }

            NewGhostMemberBottomSheet newGhostMemberBottomSheet = new NewGhostMemberBottomSheet();
            newGhostMemberBottomSheet.setGroupType("permgrp");

            // listen for nickname input from ghost member sheet
            newGhostMemberBottomSheet.setOnNicknameAddedListener(nickname -> {
                dismiss();
                if (parentListener != null) {
                    parentListener.onNicknameAdded(nickname);
                }
            });
            newGhostMemberBottomSheet.show(getParentFragmentManager(), "NewGhostMemberBottomSheet");
        });

        // handle adding existing friends to the group
        addExistingMemberButton.setOnClickListener(v -> {
            if (parentListener == null) {
                dismiss();
                return;
            }

            FriendsBottomSheet friendsBottomSheet = new FriendsBottomSheet();
            // listen for those friends selected and notify parent
            friendsBottomSheet.setOnFriendsSelectedListener(selectedFriends -> {
                dismiss();
                if (parentListener != null) {
                    for (Map.Entry<String, String> entry : selectedFriends.entrySet()) {
                        String friendName = entry.getValue(); // get display name
                        parentListener.onNicknameAdded(friendName);
                    }
                }
            });
            friendsBottomSheet.show(getParentFragmentManager(), "FriendsBottomSheet");
        });
    }

    public void setParentNicknameListener(BaseGroupFragment.OnNicknameAddedListener listener) {
        this.parentListener = listener;
    }
}
