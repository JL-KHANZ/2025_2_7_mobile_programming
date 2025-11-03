package com.example.mobile_programming_2025_2;

import android.os.Bundle;
import android.view.MotionEvent;

import com.example.mobile_programming_2025_2.ui.GooglyEyesView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mobile_programming_2025_2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_calendar, R.id.navigation_home, R.id.navigation_chart, R.id.navigation_chat, R.id.navigation_user)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN
                || ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            GooglyEyesView eyes = findViewById(R.id.googlyEyes);
            if (eyes != null) {
                eyes.lookAtRaw(ev.getRawX(), ev.getRawY());
            }
        }
        return super.dispatchTouchEvent(ev); // don't consume; let app handle touches normally
    }

}