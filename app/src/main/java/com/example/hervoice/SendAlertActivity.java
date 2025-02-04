package com.example.hervoice;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

public class SendAlertActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    private Button sendAlertButton;
    private TextView successText;
    private LinearLayout successMessageLayout;
    private MapView mapView;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference contactsRef;
    private List<String> contactNumbers = new ArrayList<>();
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_alert);

        sendAlertButton = findViewById(R.id.send_alert_button);
        successText = findViewById(R.id.success_text);
        successMessageLayout = findViewById(R.id.success_message_layout);
        mapView = findViewById(R.id.map_view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        contactsRef = FirebaseDatabase.getInstance().getReference("users").child("your_user_id").child("contacts");

        // Fetch contacts from Firebase
        fetchContacts();

        // Check and request permissions
        checkPermissions();

        // Send alert button click listener
        sendAlertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null && !contactNumbers.isEmpty()) {
                    sendAlert();
                } else {
                    Toast.makeText(SendAlertActivity.this, "Location or contacts not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Check permissions for location and SMS
    private void checkPermissions() {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Check for SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        }
    }

    // Fetch contacts from Firebase database
    private void fetchContacts() {
        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                contactNumbers.clear();
                for (DataSnapshot contactSnapshot : dataSnapshot.getChildren()) {
                    String phoneNumber = contactSnapshot.child("phone").getValue(String.class);
                    if (phoneNumber != null) {
                        contactNumbers.add(phoneNumber);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SendAlertActivity.this, "Failed to fetch contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Get the current location of the user
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                        } else {
                            Toast.makeText(SendAlertActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Send alert to contacts with the current location
    private void sendAlert() {
        String alertMessage = "Emergency Alert! I'm in danger. My current location is: https://maps.google.com/?q=" +
                currentLocation.getLatitude() + "," + currentLocation.getLongitude();

        for (String contactNumber : contactNumbers) {
            sendSms(contactNumber, alertMessage);
        }

        // Show success message
        successMessageLayout.setVisibility(View.VISIBLE);
        successText.setText("Alert sent successfully!");
    }

    // Send SMS to a contact (replace with actual SMS sending logic)
    private void sendSms(String phoneNumber, String message) {
        // Implement SMS sending here, for now, just a Toast
        // You can use the SMSManager API to send the message, or any other method
        Toast.makeText(SendAlertActivity.this, "Sent alert to " + phoneNumber, Toast.LENGTH_SHORT).show();
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS permission granted, continue
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
