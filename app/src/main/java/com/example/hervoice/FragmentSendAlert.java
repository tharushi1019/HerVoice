package com.example.hervoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Looper;
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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** @noinspection deprecation*/
public class FragmentSendAlert extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private ProgressBar progressBar;
    private TextView successText;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private double userLatitude = 0.0, userLongitude = 0.0;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    // Added for real-time location updates
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;

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

        // Setup location updates
        setupLocationUpdates();

        // Initialize Vibrator for Alert
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize Google Maps
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Refresh Location Button - Forces a single location update
        refreshButton.setOnClickListener(v -> {
            if (requestingLocationUpdates) {
                Toast.makeText(getActivity(), "Already receiving location updates", Toast.LENGTH_SHORT).show();
            } else {
                getUserLocation();
            }
        });

        // Send Alert Button Click
        sendAlertButton.setOnClickListener(v -> sendEmergencyAlert());

        return rootView;
    }

    // Setup real-time location updates
    private void setupLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isAdded()) {
                    return;
                }
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    updateMap(userLatitude, userLongitude);
                }
            }
        };
    }

    // Start receiving location updates
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        requestingLocationUpdates = true;
        if (isAdded()) {
            Toast.makeText(getActivity(), "Real-time location tracking enabled", Toast.LENGTH_SHORT).show();
        }
    }

    // Stop location updates
    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            if (isAdded()) {
                Toast.makeText(getActivity(), "Location tracking stopped", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        try {
            // Enable my location button if permission is granted
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }
            // Get user location once map is ready
            getUserLocation();
            // Start real-time location updates
            startLocationUpdates();
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(getActivity(), "Map initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (!requestingLocationUpdates) {
            startLocationUpdates();
        }
        if (googleMap != null && userLatitude != 0.0 && userLongitude != 0.0) {
            updateMap(userLatitude, userLongitude);
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        if (mapView != null) {
            mapView.onLowMemory();
        }
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    // Fetch User's Real-time Location (fallback if continuous updates not available)
    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        if (getActivity() == null || !isAdded()) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && isAdded()) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    if (googleMap != null) {
                        updateMap(userLatitude, userLongitude);
                    }
                } else if (isAdded()) {
                    Toast.makeText(getActivity(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(getActivity(), "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(getActivity(), "Location service error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateMap(double latitude, double longitude) {
        if (googleMap == null || getActivity() == null || !isAdded()) {
            return;
        }
        try {
            LatLng userLocation = new LatLng(latitude, longitude);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(getActivity(), "Map update error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Send Emergency Alert to Enabled Contacts
    private void sendEmergencyAlert() {
        if (getActivity() == null) return;
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
            if (getActivity() == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> alertContacts = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Boolean isSmsAlertEnabled = document.getBoolean("smsAlert");
                    String phoneNumber = document.getString("phone");
                    if (isSmsAlertEnabled != null && isSmsAlertEnabled && phoneNumber != null && !phoneNumber.isEmpty()) {
                        alertContacts.add(phoneNumber);
                    }
                }
                if (!alertContacts.isEmpty()) {
                    // Before sending SMS, fetch the latest location
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener(location -> {
                                if (location != null) {
                                    userLatitude = location.getLatitude();
                                    userLongitude = location.getLongitude();
                                    updateMap(userLatitude, userLongitude);
                                }
                                sendSMS(alertContacts);
                            });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "No contacts enabled for alerts", Toast.LENGTH_SHORT).show();
                }
            } else {
                progressBar.setVisibility(View.GONE);
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to fetch contacts";
                Toast.makeText(getActivity(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Send SMS with User's Location & Play Alert Sound
    private void sendSMS(List<String> contacts) {
        if (getActivity() == null) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            progressBar.setVisibility(View.GONE);
            return;
        }
        try {
            // Create Google Maps link with location
            String googleMapsLink = String.format(Locale.ENGLISH,
                    "https://maps.google.com/?q=%f,%f", userLatitude, userLongitude);

            // Create enhanced emergency message with coordinates and link
            String locationMessage = String.format(Locale.ENGLISH,
                    "🚨 EMERGENCY! I need help! My current location: %f, %f\n\nTrack me here: %s",
                    userLatitude, userLongitude, googleMapsLink);

            SmsManager smsManager = SmsManager.getDefault();
            for (String phoneNumber : contacts) {
                try {
                    String normalizedPhone = phoneNumber.replaceAll("[\\s-()]", "");
                    // Handle long messages by dividing them into parts
                    ArrayList<String> messageParts = smsManager.divideMessage(locationMessage);
                    smsManager.sendMultipartTextMessage(normalizedPhone, null, messageParts, null, null);
                } catch (Exception e) {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Failed to send to " + phoneNumber, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            // Play alert sound
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(getActivity(), R.raw.alert_sound);
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> {
                        if (mediaPlayer != null) {
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Could not play alert sound", Toast.LENGTH_SHORT).show();
                }
            }
            // Vibration feedback
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(500);
            }
            // Show success message
            progressBar.setVisibility(View.GONE);
            successText.setVisibility(View.VISIBLE);
            successText.setText(String.format(Locale.ENGLISH, "Alert sent to %d contacts", contacts.size()));
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Failed to send alert", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
                startLocationUpdates();
            } else {
                Toast.makeText(getActivity(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencyAlert();
            } else {
                Toast.makeText(getActivity(), "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
