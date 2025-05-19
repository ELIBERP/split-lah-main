package com.example.split_lah;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.split_lah.login_system.Login;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity displays the splash screen when the app launches
 */
public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        // Splash screen displays for 1 sec before changing screens
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if the user is logged in
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    // if not logged in, redirect to Login
                    Intent intent = new Intent(SplashActivity.this, Login.class);
                    startActivity(intent);
                } else {
                    // if logged in, redirect to Main Activity
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish();  // Close SplashActivity so user cannot go back to it
            }
        }, 1000);
    }
}
