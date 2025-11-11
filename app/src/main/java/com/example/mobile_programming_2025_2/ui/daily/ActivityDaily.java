package com.example.mobile_programming_2025_2.ui.daily;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.ui.CircularSliderView;

public class ActivityDaily extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_1_slider);

        CircularSliderView slider = findViewById(R.id.circleSlider);
        slider.setOnValueChangeListener(value -> {
            // value is 0..100
            // e.g., update a TextView, send to ViewModel, etc.
            System.out.println("value => " + value);
        });
    }
}