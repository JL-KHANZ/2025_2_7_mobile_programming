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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ActivityDailyResult extends AppCompatActivity {

    public static final String ANALYSIS_RESULT = "ANALYSIS_RESULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_3_result);

        HashMap<String, Object> combinedData = (HashMap<String, Object>) getIntent().getSerializableExtra(ANALYSIS_RESULT);
        Map<String, Integer> emotionResult = (Map<String, Integer>) combinedData.get("emotion_data");
        String feedbackResult = (String) combinedData.get("feedback_data");

        displayEmotionResults(emotionResult);
        displayFeedbackResults(feedbackResult);

        sendToDB(emotionResult, feedbackResult);

        setupActionButtons();
    }

    private void displayEmotionResults(Map<String, Integer> result) {
        if (result == null || result.isEmpty()) {
            System.out.println("result is null or empty");
            return;
        }

        LinearLayout uiResults = findViewById(R.id.daily_results_layout);
        TextView uiTopResult = findViewById(R.id.daily_result_text);
        LayoutInflater inflater = getLayoutInflater();

        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            String emotion = entry.getKey();
            int score = entry.getValue();

            View barItemView = inflater.inflate(R.layout.daily_result_emotion_bar, uiResults, false);

            TextView emotionLabel = barItemView.findViewById(R.id.emotion_label);
            View scoreBar = barItemView.findViewById(R.id.emotion_score_bar);
            TextView scoreText = barItemView.findViewById(R.id.emotion_score_text);

            emotionLabel.setText(emotion);
            scoreBar.getLayoutParams().width = (int) (score * 100);
            scoreText.setText(score);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scoreBar.getLayoutParams();
            params.width = 0;
            params.weight = score;
            scoreBar.setLayoutParams(params);

            uiResults.addView(barItemView);
        }
    }

    private void displayFeedbackResults(String result) {
        LinearLayout uiFeedbackResutls = findViewById(R.id.daily_feedback_results_layout);


    }

    private void sendToDB(Map<String, Integer> emotionResult, String feedbackResult) {

    }

    private void setupActionButtons() {
        // This is your old resultLayoutViews() method
        ImageButton btnHome = findViewById(R.id.daily_3_btn_home);
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        Button btnChat = findViewById(R.id.daily_3_search_chat_btn);
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, SearchChatActivity.class)));
    }
}
