package com.example.split_lah.ui.group;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.example.split_lah.R;

import java.util.Objects;

/**
 * Bottom sheet that allows users to add "ghost" members (members without an account).
 * Users can add multiple nicknames dynamically.
 */
public class NewGhostMemberBottomSheet extends BottomSheetDialogFragment {
    private LinearLayout nicknameContainer;
    private OnNicknameAddedListener listener;
    private Button doneButton;
    private String groupType = "tempgrp";

    // set the type of group ("permgrp" or "tempgrp")
    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_new_ghost_member, container, false);
        nicknameContainer = view.findViewById(R.id.nicknameContainer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton cancelButton = view.findViewById(R.id.cancelButton);
        Button addAnotherMemberButton = view.findViewById(R.id.addAnotherMemberButton);
        doneButton = view.findViewById(R.id.doneButton);

        // disable the Done button initially until at least one nickname field has input
        doneButton.setEnabled(false);
        doneButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.grey_500));

        cancelButton.setOnClickListener(v -> dismiss());

        // add new nickname field dynamically
        addAnotherMemberButton.setOnClickListener(v -> addNewNicknameField());

        doneButton.setOnClickListener(v -> {
            for (int i = 0; i < nicknameContainer.getChildCount(); i++) {
                View child = nicknameContainer.getChildAt(i);
                if (child instanceof EditText) {
                    String nickname = ((EditText) child).getText().toString().trim();
                    if (!nickname.isEmpty() && listener != null) {
                        listener.onNicknameAdded(nickname);
                    }
                }
            }
            dismiss();
        });

        // text watcher for the first nickname field
        EditText nicknameField1 = view.findViewById(R.id.nicknameField1);
        nicknameField1.addTextChangedListener(new NicknameTextWatcher());
    }

    /**
     * Dynamically adds a new label and EditText for entering another ghost member nickname.
     */
    private void addNewNicknameField() {
        int nicknameNumber = (nicknameContainer.getChildCount() / 2) + 1;

        // Create a new nickname label
        TextView newNicknameLabel = new TextView(getContext());
        newNicknameLabel.setText("Nickname " + nicknameNumber);
        newNicknameLabel.setTextSize(15);
        newNicknameLabel.setTypeface(Typeface.DEFAULT_BOLD);
        newNicknameLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(dpToPx(10), dpToPx(16), 0, 0);
        newNicknameLabel.setLayoutParams(labelParams);

        // Create EditText field
        EditText newNicknameField = new EditText(getContext());
        newNicknameField.setTextSize(15);
        newNicknameField.setTypeface(Typeface.DEFAULT_BOLD);
        newNicknameField.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        newNicknameField.setPadding(dpToPx(0), dpToPx(8), dpToPx(10), dpToPx(8));

        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        fieldParams.setMargins(dpToPx(10), dpToPx(8), dpToPx(16), 0);
        newNicknameField.setLayoutParams(fieldParams);

        // enable the Done button if there is text in a nickname field
        newNicknameField.addTextChangedListener(new NicknameTextWatcher());

        nicknameContainer.addView(newNicknameLabel);
        nicknameContainer.addView(newNicknameField);
    }

    /**
     * Checks all EditText fields to see if at least one has input
     * Enables or disables the Done button accordingly
     */
    private void checkIfAnyFieldHasText() {
        boolean hasText = false;

        for (int i = 0; i < nicknameContainer.getChildCount(); i++) {
            View child = nicknameContainer.getChildAt(i);
            if (child instanceof EditText) {
                if (!((EditText) child).getText().toString().trim().isEmpty()) {
                    hasText = true;
                    break;
                }
            }
        }
        doneButton.setEnabled(hasText);
        doneButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), hasText ? R.color.dark_blue : R.color.grey_500));
    }

    /**
     * TextWatcher that monitors input and triggers enabling/disabling of Done button
     */
    private class NicknameTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkIfAnyFieldHasText();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    // setter to allow parent to define what happens when a nickname is added
    public void setOnNicknameAddedListener(OnNicknameAddedListener listener) {
        this.listener = listener;
    }

    /**
     * Helper method to convert dp to pixels.
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    /**
     * Interface used to notify parent that a nickname was added.
     */
    public interface OnNicknameAddedListener {
        void onNicknameAdded(String nickname);
    }
}

