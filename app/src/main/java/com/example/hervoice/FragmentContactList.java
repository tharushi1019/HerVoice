package com.example.hervoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FragmentContactList extends Fragment {

    private RecyclerView contactsRecyclerView;
    private ContactAdapter contactAdapter;
    private TextView noContactsText;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<Contact> contactList = new ArrayList<>();
    private static final int MAX_CONTACTS = 5; // Limit to 5 contacts
    private FusedLocationProviderClient fusedLocationClient;
    private ListenerRegistration contactListener; // Firestore real-time listener
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_list, container, false);

        // Initialize UI elements
        contactsRecyclerView = rootView.findViewById(R.id.contactsRecyclerView);
        noContactsText = rootView.findViewById(R.id.no_contacts_text);
        rootView.findViewById(R.id.progress_bar_1);
        Button addContactButton = rootView.findViewById(R.id.add_contact_button);

        // Initialize Firebase and location services
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (getActivity() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        }

        // Setup RecyclerView
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        contactAdapter = new ContactAdapter(contactList,
                this::dialContact,
                this::sendSMS,
                this::deleteContact,
                this::onEditClick);
        contactsRecyclerView.setAdapter(contactAdapter);

        // Fetch contacts in real-time
        listenForContactUpdates();

        // Add Contact Button
        addContactButton.setOnClickListener(view -> {
            if (contactList.size() >= MAX_CONTACTS) {
                showToast("Maximum 5 contacts allowed");
            } else {
                if (context != null) {
                    startActivity(new Intent(context, AddContactActivity.class));
                }
            }
        });

        return rootView;
    }

    // 🔥 Real-time Firestore Listener to Auto-Update Contact List
    private void listenForContactUpdates() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showToast("User not logged in");
            return;
        }

        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");

        // Remove previous listener to avoid duplicates
        if (contactListener != null) {
            contactListener.remove();
        }

        contactListener = contactsRef.orderBy("name").addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                showToast("Error listening for contact changes");
                return;
            }

            if (querySnapshot != null) {
                contactList.clear(); // Clear existing contacts
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    Contact contact = document.toObject(Contact.class);
                    if (contact != null) {
                        contact.setContactId(document.getId());
                        contactList.add(contact);
                    }
                }
                updateUI();
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

        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");
        contactsRef.document(contact.getContactId()).delete()
                .addOnSuccessListener(aVoid -> showToast("Contact deleted"))
                .addOnFailureListener(e -> showToast("Failed to delete contact"));
    }

    private void dialContact(Contact contact) {
        if (context == null || getActivity() == null) return;

        String phoneNumber = contact.getPhone();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CALL_PHONE}, 1);
        }
    }

    /**
     * Creates emergency message with location information
     *
     * @param location The user's current location
     * @return Formatted emergency message with coordinates and map link
     */
    private String createEmergencyMessage(Location location) {
        // Create Google Maps link with real-time location
        String googleMapsLink = "https://maps.google.com/?q=" +
                location.getLatitude() + "," +
                location.getLongitude();

        // Create message with coordinates and clickable link
        return "EMERGENCY! I need help! My current location: " +
                location.getLatitude() + ", " +
                location.getLongitude() +
                "\n\nTrack me here: " + googleMapsLink;
    }

    private void sendSMS(Contact contact) {
        if (context == null || getActivity() == null || fusedLocationClient == null) return;

        // Check for SEND_SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request SEND_SMS permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.SEND_SMS}, 3);
            return;
        }

        // Check for location permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // Get emergency message from dedicated method
                            String message = createEmergencyMessage(location);
                            String phoneNumber = contact.getPhone();

                            SmsManager smsManager = SmsManager.getDefault();
                            // Handle long messages by dividing them into parts
                            ArrayList<String> messageParts = smsManager.divideMessage(message);
                            smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);

                            showToast("Emergency location sent!");
                        } else {
                            showToast("Unable to get location");
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
    }

    private void onEditClick(Contact contact) {
        if (context == null) return;

        if (contact == null || contact.getContactId() == null || contact.getContactId().isEmpty()) {
            showToast("Error: Contact not found");
            return;
        }

        Intent intent = new Intent(context, EditContactActivity.class);
        intent.putExtra("contact_id", contact.getContactId());
        startActivity(intent);
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    // 🔥 Stop listening when fragment is destroyed (to prevent memory leaks)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contactListener != null) {
            contactListener.remove();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
    }
}