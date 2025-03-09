package com.example.hervoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private ViewPager2 viewPager;
    private ImageView homeIcon, contactsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences for session management
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Check if the user is logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            startActivity(new Intent(HomeActivity.this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize ViewPager2
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));

        // Initialize navigation icons
        homeIcon = findViewById(R.id.homeIcon);
        contactsIcon = findViewById(R.id.contactsIcon);

        // Set initial icon colors
        updateIconColors(0);

        // Set up click listeners for icons
        homeIcon.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, true);
        });

        contactsIcon.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, true);
        });

        // Set up page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIconColors(position);
            }
        });

        // Initialize Logout Button
        Button logoutButton = findViewById(R.id.logout_button);

        // Logout button functionality
        logoutButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();
            mAuth.signOut();
            startActivity(new Intent(HomeActivity.this, SignInActivity.class));
            finish();
        });
    }

    // Update icon colors based on currently selected fragment
    private void updateIconColors(int position) {
        int activeColor = ContextCompat.getColor(this, R.color.white); // Replace with your app's primary color
        int inactiveColor = ContextCompat.getColor(this, R.color.gray); // Add a gray color in your colors.xml

        if (position == 0) {
            homeIcon.setImageTintList(ColorStateList.valueOf(activeColor));
            contactsIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
        } else {
            homeIcon.setImageTintList(ColorStateList.valueOf(inactiveColor));
            contactsIcon.setImageTintList(ColorStateList.valueOf(activeColor));
        }
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(@NonNull AppCompatActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new FragmentSendAlert();
            } else {
                return new FragmentContactList();
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Two fragments
        }
    }
}