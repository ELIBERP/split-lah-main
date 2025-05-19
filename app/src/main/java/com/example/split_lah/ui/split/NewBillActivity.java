package com.example.split_lah.ui.split;

import androidx.appcompat.app.AppCompatActivity;

import com.example.split_lah.R;
import com.example.split_lah.models.EqualSplit;
import com.example.split_lah.models.TransactionLine;
import com.example.split_lah.models.User;
import com.google.android.material.tabs.TabLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NewBillActivity extends AppCompatActivity implements SplitAdapter.OnCheckedUsersChangedListener, CustomSpinner.OnSpinnerEventsListener {
    private String currentGroupId = "";
    private String splitMethod = "Equally"; // Default
    private TabLayout splitMethodTabs;
    private FirebaseFirestore db;
    private RecyclerView splitList;
    private SplitAdapter splitAdapter;
    private List<User> groupUsers = new ArrayList<>();
    private Spinner currencySpinner;
    private List<String> selectedUsers = new ArrayList<>();
    private HashMap<String, String> splits;
    private EditText text_bill_price;
    private EditText text_bill_title;
    private ImageButton close_button;
    private Bundle arguments;
    private CustomSpinner spinner_paid_by;
    private PaidByAdapter paid_by_adapter;
    private CustomSpinner spinner_categories;
    private CategoriesAdapter categories_adapter;
    private boolean isTemporaryGroup = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_bill);

        arguments = getIntent().getExtras();
        if (arguments != null) {
            currentGroupId = arguments.getString("GROUP_ID");
            isTemporaryGroup = arguments.getBoolean("IS_TEMPORARY_GROUP", false);
        }

        text_bill_price = findViewById(R.id.bill_price);
        text_bill_title = findViewById(R.id.bill_title);
        close_button = findViewById(R.id.close_button);
        splitMethodTabs = findViewById(R.id.split_method_tabs);
        splitList = findViewById(R.id.split_list);
        currencySpinner = findViewById(R.id.currency_spinner);
        splitList.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        createSpinner();

        // Split adapter setup
        splitAdapter = new SplitAdapter(groupUsers);
        splitList.setAdapter(splitAdapter);
        selectedUsers = splitAdapter.getCheckedUsers();
        splitAdapter.setOnCheckedUsersChangedListener(this);

        if (isTemporaryGroup) {
            ArrayList<User> ghostMembers = arguments.getParcelableArrayList("GHOST_MEMBERS");
            if (ghostMembers != null && !ghostMembers.isEmpty()) {
                groupUsers.clear();
                groupUsers.addAll(ghostMembers);
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                db.collection("users").document(currentUserId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String firstName = documentSnapshot.getString("first_name");
                                String lastName = documentSnapshot.getString("last_name");
                                String icon = documentSnapshot.getString("icon");

                                // Create user object for current user
                                User currentUser = new User(currentUserId, firstName, lastName, "0.00", icon);

                                // Check if user already exists in the list
                                boolean userExists = false;
                                for (User user : groupUsers) {
                                    if (user.getId().equals(currentUserId)) {
                                        userExists = true;
                                        break;
                                    }
                                }
                                if (!userExists) {
                                    groupUsers.add(0, currentUser);
                                }
                                updateAdapters();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("NewBillActivity", "Error fetching current user: ", e);
                        });
            }
        } else {
            fetchGroupUsers();
        }

        setupTabListener();
        priceListener();
        setClose();
        loadCurrenciesFromFirestore();

        findViewById(R.id.save_bill).setOnClickListener(view -> saveBill());
    }


    // update adapters when group users change
    private void updateAdapters() {
        // Update split adapter
        splitAdapter.notifyDataSetChanged();

        // Update spinner adapter
        if (paid_by_adapter == null) {
            paid_by_adapter = new PaidByAdapter(this, groupUsers);
            spinner_paid_by.setAdapter(paid_by_adapter);
        } else {
            paid_by_adapter.notifyDataSetChanged();
        }

        // Select the first item if available
        if (!groupUsers.isEmpty()) {
            spinner_paid_by.setSelection(0);
        }
    }

    // Calculate split based on split method
    // when
    // 1. price changes - DONE
    // 2. split method changes
    // 3. users selected changes - DONE
    private void calculateSplit() {
        Log.d("SaveBill", "Calculating Split \n Users: " + selectedUsers);
        String priceText = text_bill_price.getText().toString().trim();

        if (!priceText.isEmpty() && Objects.equals(splitMethod, "Equally")) {
            try {
                double price = Double.parseDouble(priceText);
                splits = new EqualSplit(1, selectedUsers, price).calculateShare();
                splitAdapter.setSplits(splits);
                Log.d("SaveBill", "Documents: " + splits);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void priceListener() {
        // Add this after initializing text_bill_price
        text_bill_price.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateSplit();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setClose() {
        close_button.setOnClickListener(view -> {
            finish();
        });
    }

    // 🔹 Step 1: Handle Tab Selection for Split Methods
    private void setupTabListener() {
        splitMethodTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        splitMethod = "Equally";
                        break;
                    case 1:
                        splitMethod = "Percentage";
                        break;
                    case 2:
                        splitMethod = "Parts";
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // Fetch Users from Firebase firestore database
    private void fetchGroupUsers() {
        db.collection("permanent_grp")
                .document(currentGroupId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        List<Long> members = (List<Long>) document.get("members");
                        assert members != null;
                        Log.d("members", "members: " + members.toString());

                        db.collection("users")
                                .whereIn(FieldPath.documentId(), members)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        groupUsers.clear();
                                        for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                            String userId = document2.getId();
                                            String firstName = document2.getString("first_name");
                                            String lastName = document2.getString("last_name");
                                            String icon = document2.getString("icon");

                                            Log.d("members", "UserId: " + userId + ", FirstName: " + firstName + ", LastName: " + lastName);
                                            groupUsers.add(new User(userId, firstName, lastName, "0.00", icon));
                                        }

                                        // For splits checkbox list
                                        // splitAdapter = new SplitAdapter(groupUsers);
                                        splitList.setAdapter(splitAdapter);

                                        // For Spinner
                                        // 1️⃣  Build or refresh the adapter
                                        if (paid_by_adapter == null) {
                                            paid_by_adapter = new PaidByAdapter(this, groupUsers);
                                            spinner_paid_by.setAdapter(paid_by_adapter);
                                        } else {
                                            paid_by_adapter.notifyDataSetChanged();
                                        }

                                        // 2️⃣  Optionally select the first item so something is visible
                                        if (!groupUsers.isEmpty()) {
                                            spinner_paid_by.setSelection(0);
                                        }
                                    } else {
                                        Log.d("Firestore", "Error getting documents: ", task2.getException());
                                    }
                                });
                    } else {
                        Log.d("Firestore", "Error getting documents: ", task.getException());
                    }
                });
    }

    // 🔹 Step 3: Save Bill to Firestore
    private void saveBill() {
        try {
            Log.d("SaveBill", "In saving bill");

            // Get all fields
            String title = text_bill_title.getText().toString();
            String price = text_bill_price.getText().toString();
            String currencyCode = currencySpinner.getSelectedItem().toString();
            HashMap<String, String> finalSplits = splitAdapter.getSplits();

            String selectedCategory = "others"; // Fallback value
            if (spinner_categories != null && spinner_categories.getSelectedItem() != null) {
                selectedCategory = spinner_categories.getSelectedItem().toString();
            }

            // Get selected user from spinner
            int selectedPosition = spinner_paid_by.getSelectedItemPosition();
            if (selectedPosition < 0 || selectedPosition >= groupUsers.size()) {
                Toast.makeText(this, "Please select who paid", Toast.LENGTH_SHORT).show();
                return;
            }

            User selectedPayer = groupUsers.get(selectedPosition);
            String payer = selectedPayer.getId();

            Log.d("SaveBill", "Selected payer: " + selectedPayer.getName() + " (ID: " + payer + ")");

            HashMap<String, String> paidBy = new HashMap<>();
            paidBy.put(payer, price);

            // Check if all fields are filled
            if (title.isEmpty() || price.isEmpty() || paidBy.isEmpty() || !splitAdapter.checkIfSplitsAreBalanced(price)) {
                throw new RuntimeException("Please fill all fields!");
            }

            // Make the bill data
            HashMap<String, Object> billData = new HashMap<>();
            billData.put("title", title);
            billData.put("amount", price);
            billData.put("paid_by", paidBy);
            billData.put("split_type", splitMethod);
            billData.put("splits", finalSplits);
            billData.put("created_at", FieldValue.serverTimestamp());
            billData.put("currency_code", currencyCode);
            billData.put("icon", selectedCategory);
            billData.put("repeating_transaction", false);
            billData.put("description", "");
            billData.put("exchange_rate", "");

            // check if user is adding bill through temporary group
            String billName = title;
            if (isTemporaryGroup) {
                // pass data to BillSplitSummaryActivity without saving it
                Intent intent = new Intent(NewBillActivity.this, BillSplitSummaryActivity.class);
                intent.putExtra("billName", billName);
                intent.putExtra("totalAmount", price);
                intent.putExtra("selectedCurrency", currencyCode);
                intent.putExtra("paidBy", selectedPayer.getFirstName());
                intent.putExtra("payerId", selectedPayer.getId());
                intent.putExtra("payerIcon", selectedPayer.getIcon());

                // pass detailed split information
                ArrayList<String> memberList = new ArrayList<>();
                ArrayList<String> amountList = new ArrayList<>();
                ArrayList<String> memberIdList = new ArrayList<>();
                ArrayList<String> memberIconList = new ArrayList<>();

                for (Map.Entry<String, String> entry : finalSplits.entrySet()) {
                    String userId = entry.getKey();
                    String splitAmount = entry.getValue();

                    // find the user in the groupUsers list
                    for (User user : groupUsers) {
                        if (user.getId().equals(userId)) {
                            memberList.add(user.getFirstName());
                            memberIconList.add(user.getIcon());
                            amountList.add(splitAmount);
                            memberIdList.add(userId);
                            break;
                        }
                    }
                }

                intent.putStringArrayListExtra("memberList", memberList);
                intent.putStringArrayListExtra("memberIconList", memberIconList);
                intent.putStringArrayListExtra("amountList", amountList);
                intent.putStringArrayListExtra("memberIdList", memberIdList);

                startActivity(intent);
                finish();
                return;
            }

            // For permanent groups, save bill to Firestore
            String path = "permanent_grp/" + currentGroupId + "/transactions";

            // Save bill to Firestore
            db.collection(path).add(billData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Bill saved successfully!", Toast.LENGTH_SHORT).show();

                        // Count how many transaction lines need to be created
                        int totalTransactions = 0;
                        int processedTransactions = 0;

                        for (Map.Entry<String, String> entry : finalSplits.entrySet()) {
                            String splitUserId = entry.getKey();
                            String splitAmount = entry.getValue();
                            try {
                                if (Double.parseDouble(splitAmount) > 0.0 && !splitUserId.equals(payer)) {
                                    totalTransactions++;
                                }
                            } catch (NumberFormatException e) {
                                // Skip invalid amounts
                            }
                        }

                        // Then when creating the transaction lines
                        for (Map.Entry<String, String> entry : finalSplits.entrySet()) {
                            String splitUserId = entry.getKey();
                            String splitAmount = entry.getValue();
                            try {
                                if (Double.parseDouble(splitAmount) > 0.0 && !splitUserId.equals(payer)) {
                                    TransactionLine tLine = new TransactionLine(
                                            splitAmount,
                                            payer,
                                            splitUserId,
                                            splitAmount,
                                            documentReference.getId(),
                                            currentGroupId
                                    );

                                    processedTransactions++;
                                    // Mark the last transaction
                                    if (processedTransactions == totalTransactions) {
                                        tLine.setLastTransaction(true);
                                    }

                                    // Save the transaction line
                                    saveTransactionLine(tLine);
                                }
                            } catch (NumberFormatException e) {
                                Log.e("SaveBill", "Invalid split value for user " + splitUserId + ": " + splitAmount, e);
                            }
                        }
                        // Finish the activity after all transaction lines are saved
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving bill", Toast.LENGTH_SHORT).show();
                    });
        } catch (RuntimeException e) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTransactionLine(TransactionLine transactionLine) {
        // First calculate the sum
        transactionLine.calculateSum(finalSum -> {
            // Create a map representation of the transaction line for Firestore
            HashMap<String, Object> transactionLineData = new HashMap<>();
            transactionLineData.put("amount", transactionLine.getAmount());
            transactionLineData.put("created_at", FieldValue.serverTimestamp());
            transactionLineData.put("paid_by", transactionLine.getPaid_by());
            transactionLineData.put("paid_for", transactionLine.getPaid_for());
            transactionLineData.put("sum", finalSum); // Use the calculated sum here
            transactionLineData.put("transactions_id", transactionLine.getTransactions_id());

            // Construct the path to the "transaction_lines" subcollection
            String path2 = "permanent_grp/" + currentGroupId + "/transaction_lines";

            // save to Firestore after calculation is complete
            db.collection(path2)
                    .add(transactionLineData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("SaveBill", "Transaction line saved successfully with sum: " + finalSum);

                        // Only finish the activity after the LAST transaction line is saved
                        // This ensures all transaction lines are saved before returning
                        if (transactionLine.isLastTransaction()) {
                            Toast.makeText(this, "Bill saved successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving transaction line", Toast.LENGTH_SHORT).show();
                        Log.e("saveTransactionLine", "Error: ", e);
                    });
        });
    }

    private void loadCurrenciesFromFirestore() {
        db.collection("countries_info")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Set<String> currencySet = new LinkedHashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String currencyCode = document.getString("currency_code");
                            if (currencyCode != null && currencyCode != "") {
                                currencySet.add(currencyCode);
                            }
                        }

                        // Convert the set to a list
                        List<String> currencyList = new ArrayList<>(currencySet);
                        populateCurrencySpinner(currencyList);
                    } else {
                        Toast.makeText(this, "Failed to load currencies", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Populate the Spinner with Firebase Data
    private void populateCurrencySpinner(List<String> currencies) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencies);
        currencySpinner.setAdapter(adapter);
        currencySpinner.setSelection(adapter.getPosition("SGD"));
    }

    //Save Split Method Selection
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("splitMethod", splitMethod);
    }

    @Override
    public void onCheckedUsersChanged(List<String> checkedUsers) {
        Log.d("SaveBill", "Listening to checked users: " + checkedUsers.toString());
        selectedUsers = splitAdapter.getCheckedUsers();
        calculateSplit();
    }

    private void createSpinner() {
        spinner_paid_by = findViewById(R.id.paid_by);

        spinner_paid_by.setSpinnerEventsListener(NewBillActivity.this);

        paid_by_adapter = new PaidByAdapter(NewBillActivity.this, groupUsers);
        spinner_paid_by.setAdapter(paid_by_adapter);

        spinner_categories = findViewById(R.id.categories);

        spinner_categories.setSpinnerEventsListener(NewBillActivity.this);

        // First, load the categories from resources into an ArrayList
        String[] categoriesArray = getResources().getStringArray(R.array.categories_array);
        ArrayList<String> categoriesList = new ArrayList<>();
        Collections.addAll(categoriesList, categoriesArray);

        // Then create the adapter with the correct type and parameters
        categories_adapter = new CategoriesAdapter(NewBillActivity.this, categoriesList);
        spinner_categories.setAdapter(categories_adapter);
    }

    @Override
    public void onPopupWindowOpened(Spinner spinner) {
        spinner_paid_by.setBackground(ContextCompat.getDrawable(NewBillActivity.this, R.drawable.bg_spinner_up));
        spinner_categories.setBackground(ContextCompat.getDrawable(NewBillActivity.this, R.drawable.bg_spinner_up));
    }

    @Override
    public void onPopupWindowClosed(Spinner spinner) {
        spinner_paid_by.setBackground(ContextCompat.getDrawable(NewBillActivity.this, R.drawable.bg_spinner));

        // Log the selected user for debugging
        if (spinner_paid_by.getSelectedItemPosition() >= 0 && spinner_paid_by.getSelectedItemPosition() < groupUsers.size()) {
            User selectedUser = groupUsers.get(spinner_paid_by.getSelectedItemPosition());
            Log.d("Spinner", "Selected user: " + selectedUser.getFirstName() + " " + selectedUser.getLastName());
        }

        spinner_categories.setBackground(ContextCompat.getDrawable(NewBillActivity.this, R.drawable.bg_spinner));
    }
}
