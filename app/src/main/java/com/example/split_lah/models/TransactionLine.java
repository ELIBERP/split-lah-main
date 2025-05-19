package com.example.split_lah.models;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class TransactionLine {
    private String amount;
    private Object created_at; // we set this using server timestamp
    private String paid_by;
    private String paid_for;
    private String sum;
    private String transactions_id;
    private FirebaseFirestore db;
    private String TAG = "TransactionLine";
    private String currentGroupId;
    private boolean isLastTransaction = false;

    // Constructor
    public TransactionLine(String amount, String paid_by, String paid_for, String sum, String transactions_id, String currentGroupId) {
        this.amount = amount;
        // created_at will be set using the server's timestamp
        this.paid_by = paid_by;
        this.paid_for = paid_for;
        this.sum = sum;
        this.transactions_id = transactions_id;
        this.currentGroupId = currentGroupId;
    }

    // Getters and setters
    public String getAmount() {
        return amount;
    }

    public String getPaid_by() {
        return paid_by;
    }

    public String getPaid_for() {
        return paid_for;
    }

    public String getSum() {
        return sum;
    }

    public String getTransactions_id() {
        return transactions_id;
    }

    public void setLastTransaction(boolean isLast) {
        this.isLastTransaction = isLast;
    }

    public boolean isLastTransaction() {
        return isLastTransaction;
    }

    public interface CalculationCallback {
        void onCalculationComplete(String finalSum);
    }
    
    public void calculateSum(CalculationCallback callback) {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Calculating sum for transaction line");
        
        String path = "permanent_grp/" + currentGroupId + "/transaction_lines";
        
        db.collection(path)
            .where(Filter.or(
                Filter.and(
                    Filter.equalTo("paid_by", paid_by),
                    Filter.equalTo("paid_for", paid_for)),
                Filter.and(
                    Filter.equalTo("paid_by", paid_for),
                    Filter.equalTo("paid_for", paid_by))
            ))
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(1) // Only get the most recent transaction line
            .get()
            .addOnCompleteListener(task -> {
                String finalSum;
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                    String previousSum = document.getString("sum");
                    Log.d(TAG, "Previous sum found: " + previousSum);


                        if (paid_by.equals(document.getString("paid_by")) && paid_for.equals(document.getString("paid_for"))) {
                            double newSum = Double.parseDouble(previousSum) + Double.parseDouble(getAmount());
                            finalSum = String.format("%.2f", newSum);
                            Log.d(TAG, "1) Previous sum found: " + previousSum + ", new sum: " + finalSum + ", amount: " + getAmount() + ", paid_by: " + paid_by + ", paid_for: " + paid_for);
                        }
                        else if (paid_by.equals(document.getString("paid_for")) && paid_for.equals(document.getString("paid_by")) && Double.parseDouble(previousSum) < Double.parseDouble(getAmount())) {
                            double newSum = Double.parseDouble(getAmount()) - Double.parseDouble(previousSum);
                            amount = "-" + getAmount();
                            finalSum = String.format("%.2f", newSum);
                            Log.d(TAG, "2) Previous sum found: " + previousSum + ", new sum: " + finalSum + ", amount: " + getAmount() + ", paid_by: " + paid_by + ", paid_for: " + paid_for);
                        } else {
                            paid_by = document.getString("paid_for");
                            paid_for = document.getString("paid_by");
                            double newSum = Double.parseDouble(previousSum) - Double.parseDouble(getAmount());
                            finalSum = String.format("%.2f", newSum);
                            Log.d(TAG, "3) Previous sum found: " + previousSum + ", new sum: " + finalSum + ", amount: " + getAmount() + ", paid_by: " + paid_by + ", paid_for: " + paid_for);
                        }


                } else {
                    Log.d(TAG, "No previous transaction found, using current amount as sum");
                    finalSum = sum; // No previous transaction, use current amount
                }
                
                Log.d(TAG, "Final calculated sum: " + finalSum);
                this.sum = finalSum;
                
                // Call the callback with the final sum
                callback.onCalculationComplete(finalSum);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error calculating sum: ", e);
                // If there's an error, just use the current amount
                callback.onCalculationComplete(sum);
            });
    }

}
