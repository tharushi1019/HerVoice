package com.example.hervoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Make sure this is the correct layout

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
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));

        // Initialize Buttons
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
