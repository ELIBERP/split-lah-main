package com.example.split_lah.login_system;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.split_lah.MainActivity;
import com.example.split_lah.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.widget.Switch;


public class Login extends AppCompatActivity {

    private EditText emailOrPhone, password;
    private Button btnLogin;
    private FirebaseAuth mAuth;
//    private FirebaseRemoteConfig remoteConfig;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
//        remoteConfig = FirebaseRemoteConfig.getInstance();

        // Find views
        emailOrPhone = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btn_login);

        // hving a listener in the login button to listen for any login changes
        btnLogin.setOnClickListener(v -> {
            String emailOrPhoneText = emailOrPhone.getText().toString().trim();
            String passwordText = password.getText().toString().trim();

            if (emailOrPhoneText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please enter that you entered the right login details!", Toast.LENGTH_SHORT).show();
            } else {
                // Use your custom method to handle the login
                signInWithEmailAndPassword(emailOrPhoneText, passwordText);
            }
        });

        if (getSupportActionBar() != null) { // used to hide the main bar
            getSupportActionBar().hide();
        }


        Switch switchButton = findViewById(R.id.btn_remember_me);
        switchButton.setChecked(getLoginState()); // Set the switch state based on saved preference
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveLoginState(isChecked); // Save the state when the switch is toggled
        });

        TextView registerLink = findViewById(R.id.user_register);
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
            }
        });

        TextView forgetPasswordLink = findViewById(R.id.forget_password_link);
        forgetPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, ForgetPassword.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pg_login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    private void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login success
                        Log.d("logins", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            if (user.isEmailVerified()) { // Check if email is verified
                                // Navigate to MainActivity
                                Intent intent = new Intent(Login.this, MainActivity.class);
                                startActivity(intent);
                                finish(); // Close LoginActivity
                            } else {
                                Toast.makeText(Login.this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                FirebaseAuth.getInstance().signOut(); // Sign out unverified user
                            }
                        }
                    } else {
                        // Login failed
                        Log.w("login", "signInWithEmail:failure", task.getException());
                        Toast.makeText(Login.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, navigate to MainActivity
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close LoginActivity
        }
    }

    // Save login state in SharedPreferences
    private void saveLoginState(boolean isChecked) {
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("rememberMe", isChecked);
        editor.apply();
    }

    // Retrieve login state from SharedPreferences
    private boolean getLoginState() {
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("rememberMe", false);
    }

}