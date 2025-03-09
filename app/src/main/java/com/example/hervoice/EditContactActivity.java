package com.example.hervoice;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.List;

public class EditContactActivity extends AppCompatActivity {
    private EditText contactNameEditText, contactPhoneEditText;
    private Spinner relationshipSpinner;
    private Switch smsAlertSwitch;
    private String contactId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);

        // Initialize views
        contactNameEditText = findViewById(R.id.edit_contact_name);
        contactPhoneEditText = findViewById(R.id.edit_contact_phone);
        relationshipSpinner = findViewById(R.id.edit_relationship_spinner);
        smsAlertSwitch = findViewById(R.id.sms_alert_switch);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get contactId from Intent
        contactId = getIntent().getStringExtra("contact_id"); // Fixed key name
        if (contactId == null || contactId.isEmpty()) {
            Toast.makeText(this, "Error: Contact ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up spinner options
        setupSpinner();

        // Fetch and populate contact data
        fetchContactData();

        // Save button listener
        findViewById(R.id.save_contact_button).setOnClickListener(v -> saveContactData());
    }

    private void setupSpinner() {
        List<String> relationshipOptions = Arrays.asList("Family", "Friend", "Colleague", "Other");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, relationshipOptions);
        relationshipSpinner.setAdapter(adapter);
    }

    private void fetchContactData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DocumentReference contactRef = db.collection("users").document(user.getUid()).collection("contacts").document(contactId);
        contactRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Contact contact = task.getResult().toObject(Contact.class);
                if (contact != null) {
                    contactNameEditText.setText(contact.getName());
                    contactPhoneEditText.setText(contact.getPhone());

                    // Set relationship spinner value
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) relationshipSpinner.getAdapter();
                    int position = adapter.getPosition(contact.getRelationship());
                    if (position != -1) {
                        relationshipSpinner.setSelection(position);
                    }

                    smsAlertSwitch.setChecked(contact.isSmsAlert());
                }
            } else {
                Toast.makeText(EditContactActivity.this, "Error fetching contact data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveContactData() {
        String name = contactNameEditText.getText().toString().trim();
        String phone = contactPhoneEditText.getText().toString().trim();
        String relationship = relationshipSpinner.getSelectedItem().toString();
        boolean smsAlert = smsAlertSwitch.isChecked();

        // Basic validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("\\d{10,15}")) { // Basic phone number validation
            Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create updated contact object
        Contact updatedContact = new Contact(contactId, name, phone, relationship, smsAlert);

        // Save updated data to Firestore
        db.collection("users").document(user.getUid()).collection("contacts").document(contactId)
                .set(updatedContact, SetOptions.merge()) // Merge only changed fields
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditContactActivity.this, "Contact updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> Toast.makeText(EditContactActivity.this, "Error updating contact", Toast.LENGTH_SHORT).show());
    }
}
