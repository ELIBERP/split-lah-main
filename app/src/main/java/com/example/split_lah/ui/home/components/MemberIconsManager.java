package com.example.split_lah.ui.home.components;

import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;

import com.example.split_lah.databinding.FragmentHomeBinding;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.members.Item;
import com.example.split_lah.ui.members.MembersViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for managing member icons in the home fragment
 */
public class MemberIconsManager {
    private final FragmentHomeBinding binding;
    private final LifecycleOwner lifecycleOwner;
    private final MembersViewModel membersViewModel;
    private final SharedViewModel sharedViewModel;
    private final String currentUserId;

    // UI Components
    private ShapeableImageView member1;
    private ShapeableImageView member2;
    private ShapeableImageView member3;
    private ShapeableImageView member4;
    private ShapeableImageView overlayImage;
    private TextView overlayText;

    public MemberIconsManager(
            FragmentHomeBinding binding,
            LifecycleOwner lifecycleOwner,
            MembersViewModel membersViewModel,
            SharedViewModel sharedViewModel,
            String currentUserId) {
        this.binding = binding;
        this.lifecycleOwner = lifecycleOwner;
        this.membersViewModel = membersViewModel;
        this.sharedViewModel = sharedViewModel;
        this.currentUserId = currentUserId;

        initializeViews();
    }

    private void initializeViews() {
        member1 = binding.imgHomeMember1;
        member2 = binding.imgHomeMember2;
        member3 = binding.imgHomeMember3;
        member4 = binding.imgHomeMember4;
        overlayImage = binding.imgHomeOverlay;
        overlayText = binding.tvHomeOverlay;
    }

    public void setup() {
        hideAllIcons();

        if (currentUserId != null) {
            membersViewModel.setCurrentUserId(currentUserId);
        }

        setupObservers();
    }

    private void hideAllIcons() {
        member1.setVisibility(View.GONE);
        member2.setVisibility(View.GONE);
        member3.setVisibility(View.GONE);
        member4.setVisibility(View.GONE);
        overlayImage.setVisibility(View.GONE);
        overlayText.setVisibility(View.GONE);
    }

    private void setupObservers() {
        // Observe group ID changes from SharedViewModel
        sharedViewModel.getGroupId().observe(lifecycleOwner, groupId -> {
            if (groupId != null && !groupId.isEmpty()) {
                // This will trigger data fetch in MembersViewModel
                membersViewModel.setCurrentGroupId(groupId);
            }
        });

        // Observe the members list from MembersViewModel
        membersViewModel.getItems().observe(lifecycleOwner, membersList -> {
            if (membersList == null || membersList.isEmpty()) {
                return;
            }

            // Filter out current user
            List<Item> filteredMembers = new ArrayList<>();
            for (Item member : membersList) {
                if (!member.getUserId().equals(currentUserId)) {
                    filteredMembers.add(member);
                }
            }

            int totalMemberCount = membersList.size();
            updateMemberIconsUI(filteredMembers, totalMemberCount);
        });
    }

    private void updateMemberIconsUI(List<Item> membersList, int totalMemberCount) {
        // Reset all to invisible
        hideAllIcons();

        int displayCount = Math.min(membersList.size(), 4);

        // Display up to 4 member icons
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
            if (totalMemberCount > 5) {
                int extraMembers = totalMemberCount - 5;
                overlayImage.setVisibility(View.VISIBLE);
                overlayText.setVisibility(View.VISIBLE);
                overlayText.setText("+" + extraMembers);
            }
        }
    }
}