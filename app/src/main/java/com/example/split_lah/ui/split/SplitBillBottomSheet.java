package com.example.split_lah.ui.split;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.split_lah.R;
import com.example.split_lah.models.User;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class SplitBillBottomSheet extends BottomSheetDialogFragment {
    private SharedViewModel sharedViewModel;
    private String groupId;
    private ArrayList<User> ghostMembers;

    // Update constructor to accept ghost members
    public SplitBillBottomSheet(String groupId, ArrayList<User> ghostMembers) {
        this.groupId = groupId;
        this.ghostMembers = ghostMembers != null ? ghostMembers : new ArrayList<>();
    }
    public SplitBillBottomSheet(String groupId) {
        this(groupId, null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_split_bill_bottom_sheet, container, false);

        // Get the groupId from the SharedViewModel
        if (sharedViewModel.getGroupId().getValue() != null) {
            groupId = sharedViewModel.getGroupId().getValue();
            Log.d("SplitBillBottomSheet", "groupId: " + groupId);
        } else {
            Log.e("SplitBillBottomSheet", "groupId nothinggg");
        }

        // Find LinearLayouts instead of Buttons
        LinearLayout openCamera = view.findViewById(R.id.open_camera);
        LinearLayout uploadBill = view.findViewById(R.id.upload_bill);
        LinearLayout createBill = view.findViewById(R.id.create_bill);

        // Set Click Listeners
        openCamera.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(intent);
            dismiss();
        });

        uploadBill.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivity(intent);
            dismiss();
        });

        createBill.setOnClickListener(v -> {
            if (groupId != null) {
                // pass the groupId to NewBillActivity
                Intent intent = new Intent(getActivity(), NewBillActivity.class);
                intent.putExtra("GROUP_ID", groupId);  // Pass the groupId

                // Pass ghost members if have any
                if (ghostMembers != null && !ghostMembers.isEmpty()) {
                    intent.putExtra("IS_TEMPORARY_GROUP", true);
                    intent.putParcelableArrayListExtra("GHOST_MEMBERS", ghostMembers);
                }
                startActivity(intent);
            } else {
                Log.d("SplitBillBottomSheet", "No group ID available");
            }
            dismiss();
        });

        return view;
    }
}