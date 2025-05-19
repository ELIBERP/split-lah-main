package com.example.split_lah.login_system;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.split_lah.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ForgetPassword extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetPasswordButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.email_reset);
        resetPasswordButton = findViewById(R.id.reset_password_button);

        if (getSupportActionBar() != null) { // used to hide the main bar
            getSupportActionBar().hide();
        }

        resetPasswordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                sendPasswordResetEmail(email);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkEmailInFirebase() {
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(ForgetPassword.this, "Please enter your email!", Toast.LENGTH_SHORT).show();
            return;
        }

        // check if the user is in the database
        db.collection("users").whereEqualTo("email", email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        sendPasswordResetEmail(email);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ForgetPassword.this, "Error! Please try again later!", Toast.LENGTH_SHORT).show());
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgetPassword.this, "If your email is registered, you will receive an email!", Toast.LENGTH_LONG).show();

                        // Redirect to Login Page
                        Intent intent = new Intent(ForgetPassword.this, Login.class); // Change to your login activity class
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // Close forget password activity
                    } else {
                        Toast.makeText(ForgetPassword.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
