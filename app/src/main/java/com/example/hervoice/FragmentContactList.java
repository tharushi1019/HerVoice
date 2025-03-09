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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

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
    private ListenerRegistration contactListener; // Firestore real-time listener

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_list, container, false);

        // Initialize UI elements
        contactsRecyclerView = rootView.findViewById(R.id.contactsRecyclerView);
        noContactsText = rootView.findViewById(R.id.no_contacts_text);
        progressBar = rootView.findViewById(R.id.progress_bar_1);
        Button addContactButton = rootView.findViewById(R.id.add_contact_button);

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

        // Fetch contacts in real-time
        listenForContactUpdates();

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

    // 🔥 Real-time Firestore Listener to Auto-Update Contact List
    private void listenForContactUpdates() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");

        // Remove previous listener to avoid duplicates
        if (contactListener != null) {
            contactListener.remove();
        }

        contactListener = contactsRef.orderBy("name").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot querySnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getActivity(), "Error listening for contact changes", Toast.LENGTH_SHORT).show();
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
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Contact deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Failed to delete contact", Toast.LENGTH_SHORT).show());
    }

    private void dialContact(Contact contact) {
        String phoneNumber = contact.getPhone();
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CALL_PHONE}, 1);
        }
    }

    private void sendSMS(Contact contact) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            String message = "Emergency! My location is: " + location.getLatitude() + ", " + location.getLongitude();
                            String phoneNumber = contact.getPhone();
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                            Toast.makeText(getActivity(), "Emergency message sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
    }

    private void onEditClick(Contact contact) {
        if (contact == null || contact.getContactId() == null || contact.getContactId().isEmpty()) {
            Toast.makeText(getActivity(), "Error: Contact not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), EditContactActivity.class);
        intent.putExtra("contact_id", contact.getContactId());
        startActivity(intent);
    }

    // 🔥 Stop listening when fragment is destroyed (to prevent memory leaks)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contactListener != null) {
            contactListener.remove();
        }
    }
}
