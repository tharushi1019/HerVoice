package com.example.hervoice;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class FragmentSendAlert extends Fragment {

    private static final String TAG = "FragmentSendAlert";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int SMS_PERMISSION_REQUEST = 101;

    private MapView mapView;
    private ProgressBar progressBar;
    private TextView successText;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private double userLatitude = 0.0, userLongitude = 0.0;
    private Marker userMarker;
    private Vibrator vibrator;
    private ImageButton refreshButton;
    private ImageButton sendAlertButton;
    private boolean mapInitialized = false;
    private LocationCallback locationCallback;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_send_alert, container, false);

        // Retain instance to prevent map recreation on swipe
        setRetainInstance(true);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
                requireContext(),
                requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // Initialize UI elements
        mapView = rootView.findViewById(R.id.map_view);
        sendAlertButton = rootView.findViewById(R.id.send_alert_button);
        refreshButton = rootView.findViewById(R.id.refresh_location_button);
        progressBar = rootView.findViewById(R.id.progress_bar);
        successText = rootView.findViewById(R.id.success_text);
        successText.setVisibility(View.GONE);

        // Initialize Firebase & Location Services
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Vibrator for Alert
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize Location Callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (isAdded() && locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    updateMap(userLatitude, userLongitude);
                    Toast.makeText(getActivity(), "Location updated", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "LocationResult is null");
                }
            }
        };

        // Set up click listeners
        setupClickListeners();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (checkLocationPermissions()) {
            initializeMap();
            getUserLocation();
        }
    }

    private boolean checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void initializeMap() {
        if (!isAdded() || mapView == null) return;

        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS);
            IMapController mapController = mapView.getController();
            mapController.setZoom(15.0);
            mapController.setCenter(new GeoPoint(20.0, 0.0)); // Default location

            mapInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing map", e);
        }
    }

    private void getUserLocation() {
        if (!checkLocationPermissions()) return;

        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateMap(double latitude, double longitude) {
        if (!isAdded() || mapView == null) return;

        IMapController mapController = mapView.getController();
        GeoPoint userLocation = new GeoPoint(latitude, longitude);
        mapController.setCenter(userLocation);
        mapController.setZoom(15.0);

        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setTitle("Your Location");
            mapView.getOverlays().add(userMarker);
        }

        if (userMarker != null) {
            userMarker.setPosition(userLocation);
            mapView.invalidate();
        } else {
            Log.e(TAG, "userMarker is null");
        }
    }

    private void sendEmergencyAlert() {
        if (!isAdded()) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST);
            return;
        }

        try {
            String emergencyMessage = "Emergency! My location: https://maps.google.com/?q=" + userLatitude + "," + userLongitude;
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("RECIPIENT_PHONE_NUMBER", null, emergencyMessage, null, null);

            if (vibrator != null) {
                vibrator.vibrate(500);
            }
            successText.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS", e);
        }
    }

    private void setupClickListeners() {
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Refreshing location...", Toast.LENGTH_SHORT).show();
            getUserLocation();
        });

        sendAlertButton.setOnClickListener(v -> sendEmergencyAlert());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            if (!mapInitialized) initializeMap();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
