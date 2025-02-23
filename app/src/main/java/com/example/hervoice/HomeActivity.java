package com.example.hervoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private TextView contactName1, contactName2, contactName3, contactName4, contactName5;

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences for session management
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Check if the user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            // If not logged in, redirect to SignInActivity
            Intent intent = new Intent(HomeActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
            return; // Stop further execution
        }

        // Initializing views
        contactName1 = findViewById(R.id.contact_name_1);
        contactName2 = findViewById(R.id.contact_name_2);
        contactName3 = findViewById(R.id.contact_name_3);
        contactName4 = findViewById(R.id.contact_name_4);
        contactName5 = findViewById(R.id.contact_name_5);

        ImageButton sendAlertButton = findViewById(R.id.send_alert_button);

        ImageButton phone1 = findViewById(R.id.contact_phone_1);
        ImageButton phone2 = findViewById(R.id.contact_phone_2);
        ImageButton phone3 = findViewById(R.id.contact_phone_3);
        ImageButton phone4 = findViewById(R.id.contact_phone_4);
        ImageButton phone5 = findViewById(R.id.contact_phone_5);

        ImageButton message1 = findViewById(R.id.contact_message_1);
        ImageButton message2 = findViewById(R.id.contact_message_2);
        ImageButton message3 = findViewById(R.id.contact_message_3);
        ImageButton message4 = findViewById(R.id.contact_message_4);
        ImageButton message5 = findViewById(R.id.contact_message_5);

        Button logoutButton = findViewById(R.id.logout_button);

        // Send alert button redirects to SendAlertActivity
        sendAlertButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, SendAlertActivity.class);
            startActivity(intent);
        });

        // Edit contact buttons redirect to AddContactActivity
        contactName1.setOnClickListener(view -> redirectToAddContactActivity(1));
        contactName2.setOnClickListener(view -> redirectToAddContactActivity(2));
        contactName3.setOnClickListener(view -> redirectToAddContactActivity(3));
        contactName4.setOnClickListener(view -> redirectToAddContactActivity(4));
        contactName5.setOnClickListener(view -> redirectToAddContactActivity(5));

        // Phone buttons initiate a call
        phone1.setOnClickListener(view -> dialPhone(contactName1.getText().toString()));
        phone2.setOnClickListener(view -> dialPhone(contactName2.getText().toString()));
        phone3.setOnClickListener(view -> dialPhone(contactName3.getText().toString()));
        phone4.setOnClickListener(view -> dialPhone(contactName4.getText().toString()));
        phone5.setOnClickListener(view -> dialPhone(contactName5.getText().toString()));

        // Message buttons initiate a message
        message1.setOnClickListener(view -> sendMessage(contactName1.getText().toString()));
        message2.setOnClickListener(view -> sendMessage(contactName2.getText().toString()));
        message3.setOnClickListener(view -> sendMessage(contactName3.getText().toString()));
        message4.setOnClickListener(view -> sendMessage(contactName4.getText().toString()));
        message5.setOnClickListener(view -> sendMessage(contactName5.getText().toString()));

        // Logout button functionality
        logoutButton.setOnClickListener(view -> {
            // Clear session
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            // Sign out from Firebase
            mAuth.signOut();

            // Redirect to SignInActivity
            Intent intent = new Intent(HomeActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Redirect to AddContactActivity for adding/editing contacts
    private void redirectToAddContactActivity(int contactId) {
        if (contactId <= 5) {
            Intent intent = new Intent(HomeActivity.this, AddContactActivity.class);
            intent.putExtra("contact_id", contactId);
            startActivity(intent);
        }
    }

    // Dial phone number of the selected contact
    private void dialPhone(String contactName) {
        String phoneNumber = getPhoneNumber(contactName);
        if (phoneNumber != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone number not available for " + contactName, Toast.LENGTH_SHORT).show();
        }
    }

    // Get the phone number of the contact (this can be retrieved from your database)
    private String getPhoneNumber(String contactName) {
        // Sample implementation, replace with actual data retrieval logic
        if (contactName.equals(contactName1.getText().toString())) {
            return "1234567890"; // Replace with actual number
        }
        return null; // Handle other contacts similarly
    }

    // Send a message to the selected contact (this is a simple implementation for demonstration)
    private void sendMessage(String contactName) {
        String phoneNumber = getPhoneNumber(contactName);
        if (phoneNumber != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Message functionality is not available for " + contactName, Toast.LENGTH_SHORT).show();
        }
    }
}
