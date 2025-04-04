package com.example.hervoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** @noinspection deprecation, CallToPrintStackTrace, ResultOfMethodCallIgnored */
public class ProfileActivity extends AppCompatActivity {

    private EditText editName, editPhone;
    private TextView textEmail;
    private ImageView profileImage;
    private Button deleteAccountButton;

    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    private static final String USERS_COLLECTION = "users";
    private static final String PROFILE_IMAGE_FILENAME = "profile.jpg";
    private static final String USER_SESSION = "UserSession";
    private static final String IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = getSharedPreferences(USER_SESSION, MODE_PRIVATE);

        if (currentUser == null) {
            redirectToSignIn();
            return;
        }

        // Init views
        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        textEmail = findViewById(R.id.user_email);
        profileImage = findViewById(R.id.profile_image);
        Button saveButton = findViewById(R.id.save_button);
        Button changePhotoButton = findViewById(R.id.btn_change_photo);
        deleteAccountButton = findViewById(R.id.delete_account_button);
        Button signOutButton = findViewById(R.id.sign_out_button);

        loadUserData();
        loadImageFromInternalStorage();

        saveButton.setOnClickListener(v -> updateUserData());
        changePhotoButton.setOnClickListener(v -> openFileChooser());
        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmation());
        signOutButton.setOnClickListener(v -> signOutUser());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            redirectToSignIn();
        }
    }

    private void signOutUser() {
        // Clear shared preferences (marking user as logged out)
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_LOGGED_IN, false);
        editor.apply();

        // Clear local data
        clearLocalData();

        // Sign out from Firebase Auth
        mAuth.signOut();

        // Redirect to SignInActivity with cleared backstack
        Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void redirectToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> promptReauthentication())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void promptReauthentication() {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");

        new AlertDialog.Builder(this)
                .setTitle("Confirm your password")
                .setMessage("For security reasons, please enter your password to delete your account.")
                .setView(passwordInput)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = passwordInput.getText().toString().trim();
                    if (!password.isEmpty()) {
                        deleteAccountButton.setEnabled(false);
                        deleteAccountButton.setText("Deleting...");
                        reauthenticateAndDelete(password);
                    } else {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void reauthenticateAndDelete(String password) {
        if (currentUser != null && currentUser.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(unused -> deleteUserData())
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Authentication failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        deleteAccountButton.setEnabled(true);
                        deleteAccountButton.setText("Delete Account");
                    });
        }
    }

    @SuppressLint("SetTextI18n")
    private void deleteUserData() {
        String uid = currentUser.getUid();

        db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("contacts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        deleteContacts(uid, querySnapshot.size());
                    } else {
                        deleteUserDocumentAndAccount();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error checking contacts: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    deleteAccountButton.setEnabled(true);
                    deleteAccountButton.setText("Delete Account");
                });
    }

    @SuppressLint("SetTextI18n")
    private void deleteContacts(String uid, int totalContacts) {
        final int[] deletedCount = {0};

        db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("contacts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        deleteUserDocumentAndAccount();
                        return;
                    }

                    querySnapshot.forEach(document -> document.getReference().delete()
                            .addOnSuccessListener(unused -> {
                                deletedCount[0]++;
                                if (deletedCount[0] >= totalContacts) {
                                    deleteUserDocumentAndAccount();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                        "Failed to delete contact: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                deleteAccountButton.setEnabled(true);
                                deleteAccountButton.setText("Delete Account");
                            }));
                });
    }

    @SuppressLint("SetTextI18n")
    private void deleteUserDocumentAndAccount() {
        String uid = currentUser.getUid();

        db.collection(USERS_COLLECTION)
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> currentUser.delete()
                        .addOnSuccessListener(unused1 -> {
                            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();

                            // Mark user as logged out in SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(IS_LOGGED_IN, false);
                            editor.apply();

                            // Clear all local files and preferences
                            clearLocalData();

                            // Sign out from Firebase
                            mAuth.signOut();

                            // Redirect to sign in screen
                            redirectToSignIn();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this,
                                    "Failed to delete authentication: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            deleteAccountButton.setEnabled(true);
                            deleteAccountButton.setText("Delete Account");
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to delete user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    deleteAccountButton.setEnabled(true);
                    deleteAccountButton.setText("Delete Account");
                });
    }

    private void clearLocalData() {
        // 1. Delete profile image from internal storage
        File file = new File(getFilesDir(), PROFILE_IMAGE_FILENAME);
        if (file.exists()) {
            file.delete();
        }

        // 2. Clear app's SharedPreferences
        SharedPreferences appPreferences = getSharedPreferences("HerVoicePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor appEditor = appPreferences.edit();
        appEditor.clear();
        appEditor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                profileImage.setImageBitmap(bitmap);
                saveImageToInternalStorage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        if (currentUser == null) {
            redirectToSignIn();
            return;
        }

        String uid = currentUser.getUid();

        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String email = currentUser.getEmail();
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        editName.setText(name);
                        editPhone.setText(phone);
                        textEmail.setText(email);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(profileImage);
                        }
                    } else {
                        Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateUserData() {
        if (currentUser == null) {
            redirectToSignIn();
            return;
        }

        String uid = currentUser.getUid();
        String updatedName = editName.getText().toString().trim();
        String updatedPhone = editPhone.getText().toString().trim();

        if (updatedName.isEmpty() || updatedPhone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", updatedName);
        updates.put("phone", updatedPhone);

        db.collection(USERS_COLLECTION)
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void saveImageToInternalStorage(Bitmap bitmap) {
        try {
            // Save to user's internal storage
            File file = new File(getFilesDir(), PROFILE_IMAGE_FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImageFromInternalStorage() {
        // Load from user's internal storage
        File file = new File(getFilesDir(), PROFILE_IMAGE_FILENAME);
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            profileImage.setImageBitmap(bitmap);
        }
    }
}