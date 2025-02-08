package com.example.hervoice;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddContactActivity extends Activity {

    private EditText nameInput, phoneInput;
    private Spinner relationshipSpinner;
    private Switch smsAlertSwitch;
    private Button saveButton;
    private FirebaseDatabase database;
    private DatabaseReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        contactsRef = database.getReference("contacts");

        // Initialize views
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        relationshipSpinner = findViewById(R.id.relationship_spinner);
        smsAlertSwitch = findViewById(R.id.sms_alert_switch);
        saveButton = findViewById(R.id.save_button);

        // Populate the relationship Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.relationship_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipSpinner.setAdapter(adapter);

        // Save button click listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });
    }

    // Method to save contact to Firebase
    private void saveContact() {
        // Get input values
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String relationship = relationshipSpinner.getSelectedItem().toString();
        boolean smsAlert = smsAlertSwitch.isChecked();

        // Validate inputs
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(AddContactActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Contact object
        String contactId = contactsRef.push().getKey();  // Generate unique ID for the contact
        Contact newContact = new Contact(name, phone, relationship, smsAlert);

        // Save contact to Firebase
        if (contactId != null) {
            contactsRef.child(contactId).setValue(newContact).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(AddContactActivity.this, "Contact saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity after saving
                } else {
                    Toast.makeText(AddContactActivity.this, "Failed to save contact", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
