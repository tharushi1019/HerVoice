package com.example.hervoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FragmentContactList extends Fragment {

    private RecyclerView contactsRecyclerView;
    private ContactAdapter contactAdapter;
    private TextView noContactsText;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<Contact> contactList = new ArrayList<>();
    private static final int MAX_CONTACTS = 5; // Limit to 5 contacts
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_list, container, false);

        // Initialize UI elements
        contactsRecyclerView = rootView.findViewById(R.id.contactsRecyclerView);
        noContactsText = rootView.findViewById(R.id.no_contacts_text);
        progressBar = rootView.findViewById(R.id.progress_bar_1);
        Button addContactButton = rootView.findViewById(R.id.add_contact_button);
        ImageButton refreshButton = rootView.findViewById(R.id.refresh_contacts); // Refresh button

        // Initialize Firebase and location services
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // Setup RecyclerView
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        contactAdapter = new ContactAdapter(contactList,
                this::dialContact,
                this::sendSMS,
                this::deleteContact,
                this::onEditClick);
        contactsRecyclerView.setAdapter(contactAdapter);

        // Fetch contacts
        fetchContactsFromFirestore();

        // Refresh button functionality
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Refreshing contacts...", Toast.LENGTH_SHORT).show();
            fetchContactsFromFirestore(); // Reload contacts from Firestore
        });

        // Add Contact Button
        addContactButton.setOnClickListener(view -> {
            if (contactList.size() >= MAX_CONTACTS) {
                Toast.makeText(getActivity(), "Maximum 5 contacts allowed", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(getActivity(), AddContactActivity.class));
            }
        });

        return rootView;
    }

    private void fetchContactsFromFirestore() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");
        contactsRef.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                contactList.clear();
                for (DocumentSnapshot document : task.getResult()) {
                    Contact contact = document.toObject(Contact.class);
                    if (contact != null) {
                        contact.setContactId(document.getId());
                        contactList.add(contact);
                    }
                }
                updateUI();
            } else {
                Toast.makeText(getActivity(), "Error getting contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateUI() {
        if (contactList.isEmpty()) {
            noContactsText.setVisibility(View.VISIBLE);
            contactsRecyclerView.setVisibility(View.GONE);
        } else {
            noContactsText.setVisibility(View.GONE);
            contactsRecyclerView.setVisibility(View.VISIBLE);
        }
        contactAdapter.notifyDataSetChanged();
    }

    private void deleteContact(Contact contact) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Remove contact from Firestore
        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");
        contactsRef.document(contact.getContactId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Contact deleted", Toast.LENGTH_SHORT).show();
                    fetchContactsFromFirestore(); // Refresh contacts list after deletion
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to delete contact", Toast.LENGTH_SHORT).show());
    }

    private void dialContact(Contact contact) {
        String phoneNumber = contact.getPhone();

        // Check if the app has CALL_PHONE permission
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {

            // If permission is granted, place the call directly
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            // If permission is not granted, request permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CALL_PHONE}, 1);
        }
    }

    private void sendSMS(Contact contact) {
        // Request location permissions if not granted
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Get the user's current location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // Generate the emergency message with the location
                            String message = "Emergency! My location is: " + location.getLatitude() + ", " + location.getLongitude();
                            String phoneNumber = contact.getPhone();

                            // Use SmsManager to send the SMS
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                            Toast.makeText(getActivity(), "Emergency message sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
    }


    // Handle the edit button click
    private void onEditClick(Contact contact) {
        if (contact == null || contact.getContactId() == null || contact.getContactId().isEmpty()) {
            Toast.makeText(getActivity(), "Error: Contact not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), EditContactActivity.class);
        intent.putExtra("contact_id", contact.getContactId());  // Ensure contactId is passed
        startActivity(intent);
    }
}
