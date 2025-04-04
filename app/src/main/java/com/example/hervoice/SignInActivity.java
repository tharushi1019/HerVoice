package com.example.hervoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import java.util.Objects;

/** @noinspection deprecation*/
public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Enable Firebase App Check with Play Integrity API
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences for session management
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Check if the user is already logged in
        if (isUserLoggedIn()) {
            startActivity(new Intent(SignInActivity.this, HomeActivity.class));
            finish();
        }

        // Initialize UI Elements
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        Button btnSignIn = findViewById(R.id.btn_sign_in);
        TextView tvSignUpRedirect = findViewById(R.id.tvSignUpRedirect);
        TextView tvForgetPassword = findViewById(R.id.forget_password);

        // Sign-In Button Click Listener
        btnSignIn.setOnClickListener(view -> {
            // Disable the button to prevent multiple clicks
            btnSignIn.setEnabled(false);
            changeButtonAppearance(btnSignIn); // Change button appearance to show it's disabled
            signInUser(btnSignIn);
        });

        // Redirect to Sign Up page
        tvSignUpRedirect.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        // Forgot Password Click Listener
        tvForgetPassword.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email");
                return;
            }
            sendPasswordResetEmail(email);
        });
    }

    private void signInUser(Button btnSignIn) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            restoreButtonAppearance(btnSignIn); // Restore button appearance if validation fails
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your password");
            restoreButtonAppearance(btnSignIn); // Restore button appearance if validation fails
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserSession();
                        Toast.makeText(SignInActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignInActivity.this, "Login Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        restoreButtonAppearance(btnSignIn); // Restore button appearance if login fails
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignInActivity.this, "Password reset email sent! Check your inbox.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignInActivity.this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserSession() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    // Method to change button appearance when disabled
    private void changeButtonAppearance(Button btnSignIn) {
        btnSignIn.setBackgroundColor(getResources().getColor(R.color.gray)); // Disabled background color
        btnSignIn.setTextColor(getResources().getColor(R.color.white)); // Disabled text color
        btnSignIn.setText(R.string.please_wait); // Optional: change button text
    }

    // Method to restore button appearance when enabled
    private void restoreButtonAppearance(Button btnSignIn) {
        btnSignIn.setEnabled(true); // Enable the button
        btnSignIn.setBackgroundColor(getResources().getColor(R.color.colorPrimary)); // Normal background color
        btnSignIn.setTextColor(getResources().getColor(R.color.white)); // Normal text color
        btnSignIn.setText(R.string.sign_in_button); // Restore original button text
    }
}
