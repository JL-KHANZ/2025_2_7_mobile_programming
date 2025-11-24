// CREATE A NEW FILE: ActivityDailyResult.java
package com.example.mobile_programming_2025_2.ui.daily;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.LocalRepository;
import com.example.mobile_programming_2025_2.MainActivity;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.SearchChatActivity;
import com.example.mobile_programming_2025_2.Service.DailyEntryService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityDailyResult extends AppCompatActivity {

    public static final String ANALYSIS_RESULT = "ANALYSIS_RESULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_3_result);

        HashMap<String, Object> combinedData = (HashMap<String, Object>) getIntent().getSerializableExtra(ANALYSIS_RESULT);
        Map<String, Object> emotionResult = (Map<String, Object>) combinedData.get("emotion_data");
        Object feedbackResult = combinedData.get("feedback_data");
        String content = (String) combinedData.get("content_data");

        displayEmotionResults(emotionResult);
        displayFeedbackResults(feedbackResult);

        sendToDB(emotionResult, feedbackResult, content);

        setupActionButtons();
    }

    private void displayEmotionResults(Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            System.out.println("result is null or empty");
            return;
        }

        LinearLayout uiResults = findViewById(R.id.daily_results_layout);
        TextView uiTopResult = findViewById(R.id.daily_result_text);

        LayoutInflater inflater = getLayoutInflater();

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String emotion = entry.getKey();
            Object score = entry.getValue();

            if (score instanceof  Number) {
                Number scoreNumb = (Number) score;
                float scoreFloat = scoreNumb.floatValue();
                View barItemView = inflater.inflate(R.layout.daily_result_emotion_bar, uiResults, false);

                TextView emotionLabel = barItemView.findViewById(R.id.emotion_label);
                View scoreBar = barItemView.findViewById(R.id.emotion_score_bar);
                TextView scoreText = barItemView.findViewById(R.id.emotion_score_text);

                emotionLabel.setText(emotion);
                scoreBar.getLayoutParams().width = (int) (scoreFloat * 100);
                scoreText.setText(String.format("%.1f", scoreFloat));

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scoreBar.getLayoutParams();
                params.width = 0;
                params.weight = scoreFloat;
                scoreBar.setLayoutParams(params);

                uiResults.addView(barItemView);
            }
            else {
                uiTopResult.setText(String.valueOf(score));
            }

        }
    }

    private void displayFeedbackResults(Object result) {
        LinearLayout uiFeedbackResults = findViewById(R.id.daily_feedback_results_layout);

        Map<String, Object> feedbackResult = (Map<String, Object>) result;
        String empathy = String.valueOf(feedbackResult.get("공감"));
        String mindset = String.valueOf(feedbackResult.get("마인드셋 방법"));
        String feedback1 = String.valueOf(feedbackResult.get("생활 루틴 1"));
        String feedback2 = String.valueOf(feedbackResult.get("생활 루틴 2"));

        ArrayList<String> feedbackData = new ArrayList<>(List.of(empathy, mindset, feedback1, feedback2));
        ArrayList<String> feedbackTitle = new ArrayList<>(List.of("공감", "마인드셋 방법", "생활 루틴 1", "생활 루틴 2"));

        for (int i = 0; i < feedbackData.size(); i++) {
            String data = feedbackData.get(i);
            String title = feedbackTitle.get(i);

            TextView uiTitle = new TextView(ActivityDailyResult.this);
            TextView uiContent = new TextView(ActivityDailyResult.this);

            uiTitle.setTextSize(10);
            uiTitle.setPadding(0, 20, 0, 5);
            uiTitle.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

            if (title.equals("마인드셋 방법")) {
                title = "마인드셋";
                uiTitle.setText(title);
            } else if (title.equals("생활 루틴 1") || title.equals("생활 루틴 2")) {
                title = "생활 루틴";
            }

            uiContent.setText(data);
            uiTitle.setTextSize(20);
            uiTitle.setPadding(0, 0, 0, 0);

            uiFeedbackResults.addView(uiTitle);
            uiFeedbackResults.addView(uiContent);
        }
    }

    private void sendToDB(Map<String, Object> emotionResult, Object feedbackResult, String content) {
        System.out.println("sending data to database");
        LocalRepository localRepository = new LocalRepository(new DailyEntryService());
        localRepository.setDate();
        localRepository.upsertTodayContent(content);
        localRepository.upsertTodayEmotion(emotionResult);
        localRepository.upsertTodayFeedback(feedbackResult);
    }

    private void setupActionButtons() {
        // This is your old resultLayoutViews() method
        ImageButton btnHome = findViewById(R.id.daily_3_btn_home);
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        Button btnChat = findViewById(R.id.daily_3_search_chat_btn);
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, SearchChatActivity.class)));
    }
}
