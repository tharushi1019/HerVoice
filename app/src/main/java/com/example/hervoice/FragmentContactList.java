package com.example.hervoice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FragmentContactList extends Fragment {

    private LinearLayout contactsContainer;
    private TextView noContactsText;
    private Button addContactButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Contact> contactList = new ArrayList<>();

    private static final int MAX_CONTACTS = 5; // Limit to 5 contacts

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_contact_list, container, false);

        // Find the views
        contactsContainer = rootView.findViewById(R.id.contactsContainer);
        noContactsText = rootView.findViewById(R.id.no_contacts_text);
        addContactButton = rootView.findViewById(R.id.add_contact_button);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fetchContactsFromFirestore();

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

        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");
        contactsRef.get().addOnCompleteListener(task -> {
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

    private void updateUI() {
        contactsContainer.removeAllViews();

        if (contactList.isEmpty()) {
            noContactsText.setVisibility(View.VISIBLE);
        } else {
            noContactsText.setVisibility(View.GONE);
            for (int i = 0; i < contactList.size(); i++) {
                Contact contact = contactList.get(i);
                View contactView = getLayoutInflater().inflate(R.layout.contact_layout, contactsContainer, false);

                TextView contactName = contactView.findViewById(R.id.contact_name_1);
                contactName.setText(contact.getName());

                contactView.findViewById(R.id.contact_phone_1).setOnClickListener(view -> dialContact(contact.getPhone()));
                contactView.findViewById(R.id.contact_message_1).setOnClickListener(view -> sendSMS(contact.getPhone()));

                contactsContainer.addView(contactView);
            }
        }
    }

    private void dialContact(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    private void sendSMS(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", "Emergency! Please respond.");
        startActivity(intent);
    }
}
