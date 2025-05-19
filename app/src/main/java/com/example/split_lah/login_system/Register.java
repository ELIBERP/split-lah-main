package com.example.split_lah.login_system;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.split_lah.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Register extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Spinner spinnerPhoneCodes;
    private ArrayList<String> phoneCodesList;
    private EditText firstNameEditText, lastNameEditText, emailEditText, phoneEditText, passwordEditText, confirmPasswordEditText, otpEditText;
    private Button sendOtpButton, verifyOtpButton, registerButton, backToLoginButton;
    private String generatedOtp;
    private boolean isOtpVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // initialize database
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // initialize UI elements
        spinnerPhoneCodes = findViewById(R.id.spinner_phone_codes);
        phoneCodesList = new ArrayList<>();
        firstNameEditText = findViewById(R.id.first_name);
        lastNameEditText = findViewById(R.id.last_name);
        emailEditText = findViewById(R.id.reg_email);
        phoneEditText = findViewById(R.id.reg_phone);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        otpEditText = findViewById(R.id.otp);

        sendOtpButton = findViewById(R.id.send_otp);
        verifyOtpButton = findViewById(R.id.verify_otp);
        registerButton = findViewById(R.id.register);
        backToLoginButton = findViewById(R.id.back_to_login);

        sendOtpButton.setOnClickListener(v -> sendVerificationCode(emailEditText.getText().toString().trim()));
        verifyOtpButton.setOnClickListener(v -> verifyOtp(emailEditText.getText().toString().trim(), otpEditText.getText().toString().trim()));
        registerButton.setOnClickListener(v -> registerUser());
        backToLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, Login.class);
            startActivity(intent);
            finish();
        });

        otpEditText.setVisibility(View.GONE);
        verifyOtpButton.setVisibility(View.GONE);
        registerButton.setEnabled(false);


        fetchData();

        if (getSupportActionBar() != null) { // used to hide the main bar for now
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pg_register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchData() {
        CollectionReference callcodes = db.collection("countries_info");

        callcodes.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                // Using HashSet to remove duplicates
                HashSet<String> uniquePhoneCodes = new HashSet<>();

                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    String phone_code = documentSnapshot.getString("phone_code");

                    if (phone_code != null) {
                        uniquePhoneCodes.add(phone_code); // Store the phone code without '+'
                    }
                }

                // Convert HashSet to ArrayList
                ArrayList<String> phoneCodesListWithoutPlus = new ArrayList<>(uniquePhoneCodes);

                // Sort the phone codes as numbers
                Collections.sort(phoneCodesListWithoutPlus, new Comparator<String>() {
                    @Override
                    public int compare(String code1, String code2) {
                        // Parse the phone codes as integers for numeric sorting
                        return Integer.compare(Integer.parseInt(code1), Integer.parseInt(code2));
                    }
                });

                // Add '+' symbol after sorting
                ArrayList<String> finalPhoneCodesList = new ArrayList<>();
                for (String phoneCode : phoneCodesListWithoutPlus) {
                    finalPhoneCodesList.add("+" + phoneCode); // Add '+' after sorting
                }

                // Clear the original list and add the final list with sorted phone codes
                phoneCodesList.clear();
                phoneCodesList.addAll(finalPhoneCodesList);

                setupSpinner();
            } else {
                Log.d("Data Retrieval", "Firestore data retrieval failed");
            }
        }).addOnFailureListener(e -> {
            Log.d("Data Retrieval", "Errors getting document: " + e);
        });
    }


    private void setupSpinner() {
        if (phoneCodesList.isEmpty()) {
            Log.d("Data Retrieval", "No phone codes available.");
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, phoneCodesList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPhoneCodes.setAdapter(adapter);
            Log.d("Data Retrieval", "Spinner setup with phone codes.");
        }
    }

    private void sendVerificationCode(String email) {
        generatedOtp = String.format("%06d", new Random().nextInt(1000000));

        Map<String, Object> codeData = new HashMap<>();
        codeData.put("code", generatedOtp);
        codeData.put("timestamp", System.currentTimeMillis());

        db.collection("email_verifications").document(email).set(codeData)
                .addOnSuccessListener(aVoid -> {
                    sendEmail(email, generatedOtp);

                    // Show OTP input and verify button after sending OTP
                    otpEditText.setVisibility(View.VISIBLE);
                    verifyOtpButton.setVisibility(View.VISIBLE);
                    findViewById(R.id.otp).setVisibility(View.VISIBLE);
                    Toast.makeText(Register.this, "OTP sent! Check your email.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e("OTP", "Error saving OTP: " + e.getMessage()));
    }

    private static void sendEmail(String receiver, String code) {
        Log.d("Email", "Attempting to send email to: " + receiver);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.d("Email", "Creating email session...");
                String sender = "splitlah.sutd@gmail.com";
                String password = "";

                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "465");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(sender, password);
                    }
                });

                Log.d("Email", "Creating email message...");
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(sender));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver));
                message.setSubject("Your Verification Code");
                message.setText("Welcome to SplitLah!\nYour verification code is: " + code);

                Log.d("Email", "Sending email...");
                Transport.send(message);
                Log.d("Email", "Verification email sent successfully.");
            } catch (MessagingException e) {
                Log.e("Email Error", "Failed to send email: " + e.getMessage());
            }
        });
    }

    private void verifyOtp(String email, String enteredOtp) {
        db.collection("email_verifications").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedOtp = documentSnapshot.getString("code");
                        Long timestamp = documentSnapshot.getLong("timestamp");
                        long currentTime = System.currentTimeMillis();
                        long otpValidityPeriod = 5 * 60 * 1000; // 5 minutes

                        if (timestamp != null && (currentTime - timestamp) > otpValidityPeriod) {
                            // OTP expired, delete it
                            db.collection("email_verifications").document(email).delete()
                                    .addOnSuccessListener(aVoid -> Log.d("OTP", "Expired OTP deleted."))
                                    .addOnFailureListener(e -> Log.e("OTP", "Failed to delete expired OTP: " + e.getMessage()));

                            Toast.makeText(Register.this, "OTP expired. Please request a new one.", Toast.LENGTH_SHORT).show();
                        } else if (storedOtp != null && storedOtp.equals(enteredOtp)) {
                            isOtpVerified = true;
                            registerButton.setEnabled(true);
                            Toast.makeText(Register.this, "OTP verified! You can now register.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Register.this, "Invalid OTP.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Register.this, "No OTP found for this email.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("OTP", "Error verifying OTP: " + e.getMessage()));
    }


    private void registerUser() {
        if (!isOtpVerified) {
            Toast.makeText(this, "Please verify OTP before registering.", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phoneCode = spinnerPhoneCodes.getSelectedItem().toString();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Mark email as verified manually
                            user.reload(); // Reload user to ensure latest details are fetched
                            user.updateEmail(email).addOnSuccessListener(aVoid -> {
                                user.reload();
                                user.sendEmailVerification(); // This step can be skipped if not needed
                            });

                            // Save user to Firestore
                            saveUserToFirestore(user.getUid(), firstName, lastName, email, phoneCode, phone);

                            Toast.makeText(Register.this, "Registration successful! Welcome to splitlah!", Toast.LENGTH_SHORT).show();

                            // Redirect to login page
                            startActivity(new Intent(Register.this, Login.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String userID, String firstName, String lastName, String email, String phoneCode, String phone) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("first_name", firstName);
        userMap.put("last_name", lastName);
        userMap.put("email", email);
        userMap.put("phone_code", phoneCode);
        userMap.put("phone", phone);
        userMap.put("created_at", FieldValue.serverTimestamp());

        db.collection("users").document(userID).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Register.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

