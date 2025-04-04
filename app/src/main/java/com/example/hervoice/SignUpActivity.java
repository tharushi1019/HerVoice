package com.example.hervoice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** @noinspection deprecation*/
public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";

    private EditText inputName, inputEmail, inputPhone, inputPassword, inputConfirmPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPhone = findViewById(R.id.input_phone);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        Button btnSignUp = findViewById(R.id.btn_signup);
        TextView tvSignInRedirect = findViewById(R.id.tvSignInRedirect);

        // Sign Up button click event
        btnSignUp.setOnClickListener(view -> {
            // Disable the button to prevent multiple clicks
            btnSignUp.setEnabled(false);
            changeButtonAppearance(btnSignUp); // Change button appearance to show it's disabled
            registerUser(btnSignUp);
        });

        // Redirect to Sign In page
        tvSignInRedirect.setOnClickListener(view -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser(Button btnSignUp) {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            inputName.setError("Name is required");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Email is required");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Enter a valid email");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            inputPhone.setError("Phone number is required");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (phone.length() < 10) {
            inputPhone.setError("Enter a valid phone number");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Password is required");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }
        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            restoreButtonAppearance(btnSignUp); // Restore button appearance if validation fails
            return;
        }

        // Firebase Authentication for user registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get the current user that was just created
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user profile data to Firestore
                            saveUserProfileToFirestore(user.getUid(), name, email, phone);
                        } else {
                            Toast.makeText(SignUpActivity.this, "Registration successful but failed to get user", Toast.LENGTH_SHORT).show();
                            navigateToSignIn();
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Registration Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        restoreButtonAppearance(btnSignUp); // Restore button appearance if registration fails
                    }
                });
    }

    private void saveUserProfileToFirestore(String userId, String name, String email, String phone) {
        // Store directly in the users collection
        DocumentReference userRef = db.collection("users").document(userId);

        // Create user profile data
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("email", email);
        profileData.put("phone", phone);
        profileData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore
        userRef.set(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    navigateToSignIn();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user profile data", e);
                    Toast.makeText(SignUpActivity.this, "Account created but failed to save profile data", Toast.LENGTH_SHORT).show();
                    navigateToSignIn();
                });
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    // Method to change button appearance when disabled
    private void changeButtonAppearance(Button btnSignUp) {
        btnSignUp.setBackgroundColor(getResources().getColor(R.color.gray)); // Disabled background color
        btnSignUp.setTextColor(getResources().getColor(R.color.white)); // Disabled text color
        btnSignUp.setText(R.string.processing); // Optional: change button text
    }

    // Method to restore button appearance when enabled
    private void restoreButtonAppearance(Button btnSignUp) {
        btnSignUp.setEnabled(true); // Enable the button
        btnSignUp.setBackgroundColor(getResources().getColor(R.color.colorPrimary)); // Normal background color
        btnSignUp.setTextColor(getResources().getColor(R.color.white)); // Normal text color
        btnSignUp.setText(R.string.sign_up_Button); // Restore original button text
    }
}
