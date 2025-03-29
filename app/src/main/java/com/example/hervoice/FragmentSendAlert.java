package com.example.hervoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentSendAlert extends Fragment {

    private MapView mapView;
    private ProgressBar progressBar;
    private TextView successText;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private double userLatitude = 0.0, userLongitude = 0.0;
    private Marker userMarker;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_send_alert, container, false);

        // Initialize UI elements
        mapView = rootView.findViewById(R.id.map_view);
        ImageButton sendAlertButton = rootView.findViewById(R.id.send_alert_button);
        ImageButton refreshButton = rootView.findViewById(R.id.refresh_location_button);
        progressBar = rootView.findViewById(R.id.progress_bar);
        successText = rootView.findViewById(R.id.success_text);
        successText.setVisibility(View.GONE);

        // Initialize Firebase & Location Services
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Vibrator for Alert
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize Map
        setupMap();

        // Fetch user location
        getUserLocation();

        // Refresh Location Button
        refreshButton.setOnClickListener(v -> getUserLocation());

        // Send Alert Button Click
        sendAlertButton.setOnClickListener(v -> sendEmergencyAlert());

        return rootView;
    }

    // 🌍 Initialize Map
    private void setupMap() {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
    }

    // 📍 Fetch User's Real-time Location
    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                userLatitude = location.getLatitude();
                userLongitude = location.getLongitude();
                updateMap(userLatitude, userLongitude);
            } else {
                Toast.makeText(getActivity(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🗺️ Update Map with User's Location & Marker
    private void updateMap(double latitude, double longitude) {
        GeoPoint userLocation = new GeoPoint(latitude, longitude);
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(userLocation);

        // Remove existing marker if present
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
        }

        // Add new marker at the user's location
        userMarker = new Marker(mapView);
        userMarker.setPosition(userLocation);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setTitle("You are here");

        // Set custom marker icon
        Drawable markerIcon = getResources().getDrawable(R.drawable.user_location_marker);
        userMarker.setIcon(markerIcon);

        mapView.getOverlays().add(userMarker);
        mapView.invalidate();
    }

    // 🚨 Send Emergency Alert to Enabled Contacts
    private void sendEmergencyAlert() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        successText.setVisibility(View.GONE);

        // Fetch contacts who have smsAlert set to true
        CollectionReference contactsRef = db.collection("users").document(user.getUid()).collection("contacts");
        contactsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> alertContacts = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Boolean isSmsAlertEnabled = document.getBoolean("smsAlert");
                    String phoneNumber = document.getString("phone");
                    if (isSmsAlertEnabled != null && isSmsAlertEnabled && phoneNumber != null) {
                        alertContacts.add(phoneNumber);
                    }
                }
                if (!alertContacts.isEmpty()) {
                    sendSMS(alertContacts);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "No contacts enabled for alerts", Toast.LENGTH_SHORT).show();
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Failed to fetch contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 📩 Send SMS with User's Location & Play Alert Sound
    private void sendSMS(List<String> contacts) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.SEND_SMS}, 101);
            return;
        }

        String locationMessage = String.format(Locale.ENGLISH, "🚨 Emergency Alert! I'm at: https://maps.google.com/?q=%f,%f", userLatitude, userLongitude);
        SmsManager smsManager = SmsManager.getDefault();

        for (String phoneNumber : contacts) {
            smsManager.sendTextMessage(phoneNumber, null, locationMessage, null, null);
        }

        // Play alert sound
        playAlertSound();

        // Vibrate device for alert
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.VIBRATE}, 1);
        }

        progressBar.setVisibility(View.GONE);
        successText.setVisibility(View.VISIBLE);
        Toast.makeText(getActivity(), "Emergency alert sent!", Toast.LENGTH_SHORT).show();
    }

    // 🔊 Play Emergency Alert Sound
    private void playAlertSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        mediaPlayer = MediaPlayer.create(getActivity(), R.raw.alert_sound);
        mediaPlayer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
