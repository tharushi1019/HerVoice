package com.example.hervoice;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private TextView contactName1, contactName2, contactName3, contactName4, contactName5;
    private ImageButton sendAlertButton, editContact1, editContact2, editContact3, editContact4, editContact5;
    private ImageButton phone1, phone2, phone3, phone4, phone5;
    private ImageButton message1, message2, message3, message4, message5;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initializing views
        contactName1 = findViewById(R.id.contact_name_1);
        contactName2 = findViewById(R.id.contact_name_2);
        contactName3 = findViewById(R.id.contact_name_3);
        contactName4 = findViewById(R.id.contact_name_4);
        contactName5 = findViewById(R.id.contact_name_5);

        sendAlertButton = findViewById(R.id.send_alert_button);

        editContact1 = findViewById(R.id.edit_contact_1);
        editContact2 = findViewById(R.id.edit_contact_2);
        editContact3 = findViewById(R.id.edit_contact_3);
        editContact4 = findViewById(R.id.edit_contact_4);
        editContact5 = findViewById(R.id.edit_contact_5);

        phone1 = findViewById(R.id.contact_phone_1);
        phone2 = findViewById(R.id.contact_phone_2);
        phone3 = findViewById(R.id.contact_phone_3);
        phone4 = findViewById(R.id.contact_phone_4);
        phone5 = findViewById(R.id.contact_phone_5);

        message1 = findViewById(R.id.contact_message_1);
        message2 = findViewById(R.id.contact_message_2);
        message3 = findViewById(R.id.contact_message_3);
        message4 = findViewById(R.id.contact_message_4);
        message5 = findViewById(R.id.contact_message_5);

        logoutButton = findViewById(R.id.logout_button);

        // Send alert button redirects to SendAlertActivity
        sendAlertButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, SendAlertActivity.class);
            startActivity(intent);
        });

        // Edit contact buttons redirect to AddContactActivity
        editContact1.setOnClickListener(view -> redirectToAddContactActivity(1));
        editContact2.setOnClickListener(view -> redirectToAddContactActivity(2));
        editContact3.setOnClickListener(view -> redirectToAddContactActivity(3));
        editContact4.setOnClickListener(view -> redirectToAddContactActivity(4));
        editContact5.setOnClickListener(view -> redirectToAddContactActivity(5));

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

        // Logout button redirects to SignUpActivity
        logoutButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, SignUpActivity.class);
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
