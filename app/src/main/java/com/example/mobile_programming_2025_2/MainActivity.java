package com.example.mobile_programming_2025_2;

import android.os.Bundle;
import android.view.MotionEvent;

import com.example.mobile_programming_2025_2.Service.DailyEntryService;
import com.example.mobile_programming_2025_2.ui.BubblesBackgroundView;
import com.example.mobile_programming_2025_2.ui.GooglyEyesView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
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

        LocalRepository localRepository = new LocalRepository(new DailyEntryService());
        MainViewModelFactory mainViewModelFactory = new MainViewModelFactory(localRepository);
        MainViewModel mainViewModel = new ViewModelProvider(this, mainViewModelFactory).get(MainViewModel.class);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BubblesBackgroundView bubblesBackgroundView = findViewById(R.id.background_bubbles);
        bubblesBackgroundView.setBubbleColor(getColor(R.color.default_color));

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_calendar, R.id.navigation_home, R.id.navigation_chart, R.id.navigation_chat, R.id.navigation_user)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        mainViewModel.fetchTodayData();
        mainViewModel.getTodayEntryData().observe(this, dailyEntry -> {
            if (dailyEntry != null) {
                // Update UI with the daily entry data
                // bubblesBackgroundView.setBubbleColor(dailyEntry.getColor());
            }
        });

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