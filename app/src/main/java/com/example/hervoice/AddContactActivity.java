package com.example.hervoice;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

public class AddContactActivity extends AppCompatActivity {

    private EditText nameInput, phoneInput;
    private Spinner relationshipSpinner;
    private Switch smsAlertSwitch;
    private Button saveButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private CollectionReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Reference to user's contacts collection
        String userId = auth.getCurrentUser().getUid();
        contactsRef = db.collection("users").document(userId).collection("contacts");

        // Initialize UI components
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        relationshipSpinner = findViewById(R.id.relationship_spinner);
        smsAlertSwitch = findViewById(R.id.sms_alert_switch);
        saveButton = findViewById(R.id.save_button);

        // Populate Spinner with relationship options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.relationship_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipSpinner.setAdapter(adapter);

        // Save button click event
        saveButton.setOnClickListener(v -> checkAndSaveContact());
    }

    private void checkAndSaveContact() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String relationship = relationshipSpinner.getSelectedItem().toString();
        boolean smsAlertEnabled = smsAlertSwitch.isChecked();

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
        saveButton.setText("Checking...");

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
                        saveContactToFirestore(name, phone, relationship, smsAlertEnabled);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AddContactActivity", "Error checking contact", e);
                    Toast.makeText(AddContactActivity.this, "Error checking contact!", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");
                });
    }

    private void saveContactToFirestore(String name, String phone, String relationship, boolean smsAlertEnabled) {
        // Create a HashMap to store contact data
        HashMap<String, Object> contactMap = new HashMap<>();
        contactMap.put("name", name);
        contactMap.put("phone", phone);
        contactMap.put("relationship", relationship);
        contactMap.put("smsAlertEnabled", smsAlertEnabled);

        // Add contact to Firestore
        contactsRef.add(contactMap)
                .addOnSuccessListener(documentReference -> {
                    // Success feedback
                    Toast.makeText(AddContactActivity.this, "Contact saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                })
                .addOnFailureListener(e -> {
                    // Error feedback
                    Log.e("AddContactActivity", "Failed to save contact", e);
                    Toast.makeText(AddContactActivity.this, "Failed to save contact!", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true); // Re-enable save button
                    saveButton.setText("Save");
                });
    }

    // Validates phone number (10 digits only)
    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{10}"); // Change this if needed (e.g., for other phone formats)
    }
}
