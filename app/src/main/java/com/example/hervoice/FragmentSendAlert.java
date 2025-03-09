package com.example.hervoice;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class FragmentSendAlert extends Fragment {

    private FusedLocationProviderClient fusedLocationClient;
    private MapView mapView;
    private String emergencyMessage;
    private ProgressBar progressBar;

    public FragmentSendAlert() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_send_alert, container, false);

        // Initialize the MapView
        mapView = rootView.findViewById(R.id.map_view);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // Set up the success message TextView
        TextView successText = rootView.findViewById(R.id.success_text);
        successText.setVisibility(View.GONE);  // Hide the success message initially

        // Initialize the ImageButton for sending alert
        ImageButton sendAlertButton = rootView.findViewById(R.id.send_alert_button);

        sendAlertButton.setOnClickListener(view -> {
            sendEmergencyAlert();
        });

        // Initialize ProgressBar
        progressBar = rootView.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);  // Hide initially

        // Fetch the user's real-time location
        fetchLocation();

        return rootView;
    }

    // Handle permissions request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                // Show a message to inform the user that the location permission is required
            }
        }
    }

    // Fetch the user's real-time location
    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the permissions if not granted
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        // Show ProgressBar while fetching location
        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), location -> {
                    if (location != null) {
                        // Hide ProgressBar once the location is fetched
                        progressBar.setVisibility(View.GONE);

                        // Show the user's location on the map
                        showUserLocationOnMap(location);

                        // Construct the emergency message using location
                        emergencyMessage = "Emergency! I'm at: Latitude: " + location.getLatitude() +
                                ", Longitude: " + location.getLongitude();
                    }
                });
    }

    // Show the user's location on the map
    private void showUserLocationOnMap(Location location) {
        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(new org.osmdroid.util.GeoPoint(location.getLatitude(), location.getLongitude()));
        userMarker.setIcon(getResources().getDrawable(R.drawable.ic_user_location)); // Customize your location marker
        mapView.getOverlays().add(userMarker);
    }

    // Send the emergency alert to all contacts with enabled alert
    private void sendEmergencyAlert() {
        // Show ProgressBar while sending alert
        progressBar.setVisibility(View.VISIBLE);
        // Fetch contacts from Firebase
        getContactsFromDatabase();
    }

    // Fetch contacts from Firebase
    private void getContactsFromDatabase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("contacts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Contact> trustedContacts = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Contact trustedContact = snapshot.getValue(Contact.class);
                    if (trustedContact != null && trustedContact.isSmsAlert()) {
                        trustedContacts.add(trustedContact);
                    }
                }

                // Send alert to all the trustedContacts with enabled SMS alerts
                sendAlertToContacts(trustedContacts);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Hide ProgressBar in case of error
                progressBar.setVisibility(View.GONE);
                // Optionally, show a failure message
            }
        });
    }

    // Send the emergency alert SMS to each contact
    private void sendAlertToContacts(List<Contact> trustedContacts) {
        for (Contact trustedContact : trustedContacts) {
            sendSMS(trustedContact.getPhone(), emergencyMessage);
        }

        // Hide ProgressBar after sending alerts
        progressBar.setVisibility(View.GONE);

        // Show success message
        TextView successText = getView().findViewById(R.id.success_text);
        successText.setVisibility(View.VISIBLE);
    }

    // Send SMS using SmsManager
    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            // Optionally, show an error message if SMS fails
        }
    }
}
