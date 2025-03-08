package com.example.hervoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvSignUpRedirect, tvForgetPassword;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences for session management
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Check if the user is already logged in
        if (isUserLoggedIn()) {
            // If logged in, move to HomeActivity
            startActivity(new Intent(SignInActivity.this, HomeActivity.class));
            finish();
        }

        // Initialize UI Elements
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        tvSignUpRedirect = findViewById(R.id.tvSignUpRedirect);
        tvForgetPassword = findViewById(R.id.forget_password); // Add this line

        // Sign-In Button Click Listener
        btnSignIn.setOnClickListener(view -> signInUser());

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

    private void signInUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save session information (user logged in)
                        saveUserSession(true);

                        Toast.makeText(SignInActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignInActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignInActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignInActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserSession(boolean isLoggedIn) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", isLoggedIn); // Save login status
        editor.apply();
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false); // Return login status
    }
}
