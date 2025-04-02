package com.example.hervoice;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class AddContactActivity extends AppCompatActivity {

    private EditText nameInput, phoneInput;
    private Spinner relationshipSpinner;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch smsAlertSwitch;  // Keep using smsAlert
    private Button saveButton;

    private CollectionReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Initialize Firebase instances
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Reference to user's contacts collection
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        contactsRef = db.collection("users").document(userId).collection("contacts");

        // Initialize UI components
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        relationshipSpinner = findViewById(R.id.relationship_spinner);
        smsAlertSwitch = findViewById(R.id.sms_alert_switch); // Keep smsAlert
        saveButton = findViewById(R.id.save_button);

        // Populate Spinner with relationship options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.relationship_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipSpinner.setAdapter(adapter);

        // Save button click event
        saveButton.setOnClickListener(v -> checkAndSaveContact());
    }

    @SuppressLint("SetTextI18n")
    private void checkAndSaveContact() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String relationship = relationshipSpinner.getSelectedItem().toString();
        boolean smsAlert = smsAlertSwitch.isChecked();  // Use smsAlert here

        // Validation checks
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required!");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required!");
            return;
        }
        if (!isValidPhoneNumber(phone)) {
            phoneInput.setError("Invalid phone number!");
            return;
        }

        // Disable save button to prevent multiple clicks
        saveButton.setEnabled(false);
        saveButton.setText(R.string.saving);

        // 🔥 **Step 1: Check if phone number already exists**
        contactsRef.whereEqualTo("phone", phone).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // ❌ Phone number already exists → Show error message
                        Toast.makeText(AddContactActivity.this, "This phone number is already saved!", Toast.LENGTH_SHORT).show();
                        saveButton.setEnabled(true);
                        saveButton.setText("Save");
                    } else {
                        // ✅ Phone number does not exist → Save the contact
                        saveContactToFirestore(name, phone, relationship, smsAlert);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AddContactActivity", "Error checking contact", e);
                    Toast.makeText(AddContactActivity.this, "Error checking contact!", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");
                });
    }

    @SuppressLint("SetTextI18n")
    private void saveContactToFirestore(String name, String phone, String relationship, boolean smsAlert) {
        // Check for duplicate phone number before saving
        contactsRef.whereEqualTo("phone", phone).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(AddContactActivity.this, "This phone number is already saved!", Toast.LENGTH_SHORT).show();
                        saveButton.setEnabled(true);
                        saveButton.setText("Save");
                    } else {
                        // Proceed with saving the contact as no duplicates were found
                        HashMap<String, Object> contactMap = new HashMap<>();
                        contactMap.put("name", name);
                        contactMap.put("phone", phone);
                        contactMap.put("relationship", relationship);
                        contactMap.put("smsAlert", smsAlert);  // Use smsAlert here

                        // Add contact to Firestore
                        contactsRef.add(contactMap)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(AddContactActivity.this, "Contact saved successfully!", Toast.LENGTH_SHORT).show();
                                    finish(); // Close the activity after saving
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AddContactActivity", "Failed to save contact", e);
                                    Toast.makeText(AddContactActivity.this, "Failed to save contact!", Toast.LENGTH_SHORT).show();
                                    saveButton.setEnabled(true);
                                    saveButton.setText("Save");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AddContactActivity", "Error checking contact", e);
                    Toast.makeText(AddContactActivity.this, "Error checking contact!", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");
                });
    }

    // Validates phone number (10 digits only)
    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{10}"); // Change this if needed (e.g., for other phone formats)
    }
}
