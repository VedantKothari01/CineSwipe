package com.example.cineswipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, signUpButton;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private ProgressBar progressBar;
    private View contentLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Check if user is already logged in
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);

        loginButton.setOnClickListener(v -> {
            showProgress(true);
            contentLayout.setVisibility(View.GONE);
            loginUser();
        });
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            showProgress(true);
            contentLayout.setVisibility(View.GONE);
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter both email and password.", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, start HomeActivity
                        showProgress(false);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        showProgress(false);
                        // Login failed, show an error message
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_LONG).show();
                    }
                    showProgress(false); // Hide progress bar after login attempt finishes
                })
                .addOnFailureListener(e -> {
                    // Handle login failure scenario
                    Toast.makeText(LoginActivity.this, "Authentication Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showProgress(false); // Hide progress bar on failure
                });
    }
}
