package com.example.split_lah.ui.debt_relation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.split_lah.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SettleDebtBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_FROM_NAME = "from_name";
    private static final String ARG_TO_NAME = "to_name";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_CURRENCY = "currency";

    private OnSettleConfirmedListener listener;

    public interface OnSettleConfirmedListener {
        void onSettleConfirmed(String fromName, String toName, String amount, String currency);
    }

    public static SettleDebtBottomSheetFragment newInstance(
            String fromName, String toName, String amount, String currency) {
        SettleDebtBottomSheetFragment fragment = new SettleDebtBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FROM_NAME, fromName);
        args.putString(ARG_TO_NAME, toName);
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_CURRENCY, currency);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_settle_debt, container, false);

        String fromName = getArguments().getString(ARG_FROM_NAME);
        String toName = getArguments().getString(ARG_TO_NAME);
        String amount = getArguments().getString(ARG_AMOUNT);
        String currency = getArguments().getString(ARG_CURRENCY);

        // Set up the confirmation message
        TextView messageText = view.findViewById(R.id.tv_settle_message);
        messageText.setText(String.format("Mark that %s has settled their debt of %s %s to %s?",
                fromName, currency, amount, toName));

        // Set up buttons
        Button confirmButton = view.findViewById(R.id.btn_confirm_settle);
        confirmButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSettleConfirmed(fromName, toName, amount, currency);
            }
            dismiss();
        });

        Button cancelButton = view.findViewById(R.id.btn_cancel_settle);
        cancelButton.setOnClickListener(v -> dismiss());

        return view;
    }

    public void setOnSettleConfirmedListener(OnSettleConfirmedListener listener) {
        this.listener = listener;
    }
}