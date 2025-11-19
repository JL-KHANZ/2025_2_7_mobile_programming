package com.example.mobile_programming_2025_2.ui.daily;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.MainActivity;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.SearchChatActivity;
import com.example.mobile_programming_2025_2.databinding.ActivityDaily1SliderBinding;
import com.example.mobile_programming_2025_2.network.GeminiAI.EmotionAnalysisCallback;
import com.example.mobile_programming_2025_2.network.GeminiAI.FeedbackCallback;
import com.example.mobile_programming_2025_2.network.GeminiAI;
import com.example.mobile_programming_2025_2.ui.CircularSliderView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// IN YOUR CURRENT FILE: ActivityDaily.java
public class ActivityDaily extends AppCompatActivity {

    // --- Keep your onCreate method simple ---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This Activity is now ONLY for the journal.
        setContentView(R.layout.activity_daily_2_journal);
        setupJournalButton();
    }

    // --- Create a dedicated method to set up the button ---
    private void setupJournalButton() {
        // Find views ONCE and make them final to use in the listener
        final Button btnJournal = findViewById(R.id.daily_journal_btn);
        final EditText contentText = findViewById(R.id.daily_input_content);

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
        final GeminiAI geminiAI = new GeminiAI();

        btnJournal.setOnClickListener(v -> {
            String content = contentText.getText().toString();

            if (content.trim().isEmpty()) {
                Toast.makeText(this, "Please write something first.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Analyzing...", Toast.LENGTH_SHORT).show();

            EmotionAnalysisCallback emotionCallback = new EmotionAnalysisCallback() {
                @Override
                public void onResponse(Map<String, Object> result) {
                    FeedbackCallback feedbackCallback = new FeedbackCallback() {
                        @Override
                        public void onResponse(Map<String, Object> feedback) {

                            Map<String, Object> combinedResult = new HashMap<>();
                            combinedResult.put("emotion_data", (Serializable) result);
                            combinedResult.put("feedback_data", (Serializable) feedback);

                            mainHandler.post(() -> {

                                Intent intent = new Intent(ActivityDaily.this, ActivityDailyResult.class);
                                intent.putExtra("ANALYSIS_RESULT", (Serializable) combinedResult);

                                startActivity(intent);
                            });
                        }
                        @Override
                        public void onError(Throwable throwable) {
                            mainHandler.post(() -> {
                                Toast.makeText(ActivityDaily.this, "An error occurred during feedback analysis.", Toast.LENGTH_SHORT).show();
                            });

                        }
                    };
                    geminiAI.generateFeedback(content, backgroundExecutor, feedbackCallback);
                }
                @Override
                public void onError(Throwable throwable) {
                    mainHandler.post(() -> {
                        Toast.makeText(ActivityDaily.this, "An error occurred during emotion analysis.", Toast.LENGTH_SHORT).show();
                    });
                }
            };

            geminiAI.analyzeEmotion(content, backgroundExecutor, emotionCallback);
        });
    }

}
