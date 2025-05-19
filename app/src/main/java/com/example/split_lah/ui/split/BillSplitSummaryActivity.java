package com.example.split_lah.ui.split;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.split_lah.R;

import java.util.ArrayList;

public class BillSplitSummaryActivity extends AppCompatActivity {
    private RecyclerView paymentRecyclerView;
    private BillSplitAdapter billSplitAdapter;
    private TextView billNameTextView;
    private String billName, selectedCurrency, paidBy, payerId, payerIcon;
    private ArrayList<String> memberList, amountList, memberIdList, memberIconList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_split_summary);

        // Initialize views
        billNameTextView = findViewById(R.id.bill_name);
        paymentRecyclerView = findViewById(R.id.paymentRecyclerView);

        // Retrieve passed data from Intent
        billName = getIntent().getStringExtra("billName");
        selectedCurrency = getIntent().getStringExtra("selectedCurrency");
        paidBy = getIntent().getStringExtra("paidBy");
        payerId = getIntent().getStringExtra("payerId");
        payerIcon = getIntent().getStringExtra("payerIcon");
        memberList = getIntent().getStringArrayListExtra("memberList");
        amountList = getIntent().getStringArrayListExtra("amountList");
        memberIdList = getIntent().getStringArrayListExtra("memberIdList");
        memberIconList = getIntent().getStringArrayListExtra("memberIconList");

        // Check if data was passed and update the UI
        if (billName != null) {
            billNameTextView.setText(billName);
        }

        if (memberList != null && amountList != null && !memberList.isEmpty() && !amountList.isEmpty()) {
            paymentRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Filter out the payer from the list (only show people who owe money)
            ArrayList<String> filteredNames = new ArrayList<>();
            ArrayList<String> filteredAmounts = new ArrayList<>();
            ArrayList<String> filteredIds = new ArrayList<>();
            ArrayList<String> filteredIcons = new ArrayList<>();

            for (int i = 0; i < memberList.size(); i++) {
                if (!memberIdList.get(i).equals(payerId)) {
                    filteredNames.add(memberList.get(i));
                    filteredAmounts.add(amountList.get(i));
                    filteredIds.add(memberIdList.get(i));
                    filteredIcons.add(memberIconList.get(i));
                }
            }

            billSplitAdapter = new BillSplitAdapter(
                    filteredNames,
                    filteredAmounts,
                    filteredIds,
                    payerId,
                    selectedCurrency,
                    filteredIcons,
                    payerIcon,
                    paidBy
            );
            paymentRecyclerView.setAdapter(billSplitAdapter);
        }

        findViewById(R.id.shareButton).setOnClickListener(v -> shareBillDetails());
        findViewById(R.id.cancelButton).setOnClickListener(v -> {
            finish();
        });

    }

    // Share function
    private void shareBillDetails() {
        if (billName != null && selectedCurrency != null && memberList != null && amountList != null) {
            StringBuilder shareContent = new StringBuilder();
            shareContent.append("Bill Name: ").append(billName).append("\n");
            shareContent.append("Paid by: ").append(paidBy).append("\n\n");
            shareContent.append("Split details:\n");

            for (int i = 0; i < memberList.size(); i++) {
                shareContent.append("- ").append(memberList.get(i)).append(": ")
                        .append(selectedCurrency).append(" ").append(amountList.get(i)).append("\n");
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent.toString());
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }
}