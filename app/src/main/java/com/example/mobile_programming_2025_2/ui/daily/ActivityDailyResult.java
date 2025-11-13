// CREATE A NEW FILE: ActivityDailyResult.java
package com.example.mobile_programming_2025_2.ui.daily;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.MainActivity;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.SearchChatActivity;

import java.io.Serializable;
import java.util.Map;

public class ActivityDailyResult extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_3_result);

        // 1. Get the analysis results passed from the previous activity
        Map<String, Integer> result = (Map<String, Integer>) getIntent().getSerializableExtra("ANALYSIS_RESULT");

        // 2. Display the results and set up the buttons
        displayResults(result);
        setupActionButtons();
    }

    private void displayResults(Map<String, Integer> result) {
        // This is the logic you had in your onResponse callback

        if (result == null || result.isEmpty()) {
            System.out.println("result is null or empty");
            return;
        }
        System.out.println(result);

        LinearLayout uiResults = findViewById(R.id.daily_results_layout);
        TextView uiTopResult = findViewById(R.id.daily_result_text);
        LayoutInflater inflater = getLayoutInflater();


    }

    private void setupActionButtons() {
        // This is your old resultLayoutViews() method
        ImageButton btnHome = findViewById(R.id.daily_3_btn_home);
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        Button btnChat = findViewById(R.id.daily_3_search_chat_btn);
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, SearchChatActivity.class)));
    }
}
