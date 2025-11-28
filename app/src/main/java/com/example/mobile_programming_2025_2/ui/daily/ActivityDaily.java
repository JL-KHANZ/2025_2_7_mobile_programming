package com.example.mobile_programming_2025_2.ui.daily;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.network.GeminiAI.EmotionAnalysisCallback;
import com.example.mobile_programming_2025_2.network.GeminiAI.FeedbackCallback;
import com.example.mobile_programming_2025_2.network.GeminiAI;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ActivityDaily extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_2_journal);
        setupJournalButton();
    }

    private void setupJournalButton() {
        final Button btnJournal = findViewById(R.id.daily_journal_btn);
        final EditText contentText = findViewById(R.id.daily_input_content);
        ProgressBar progressBar = findViewById(R.id.loading_spinner);

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
        final GeminiAI geminiAI = new GeminiAI();

        btnJournal.setOnClickListener(v -> {
            String content = contentText.getText().toString();

            if (content.trim().isEmpty()) {
                Toast.makeText(this, "일기를 먼저 작성해 주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnJournal.setEnabled(false);

            Toast.makeText(this, "감정 분석 중입니다...", Toast.LENGTH_LONG).show();

            EmotionAnalysisCallback emotionCallback = new EmotionAnalysisCallback() {
                @Override
                public void onResponse(Map<String, Object> result) {
                    FeedbackCallback feedbackCallback = new FeedbackCallback() {
                        @Override
                        public void onResponse(Map<String, Object> feedback) {

                            mainHandler.post(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnJournal.setEnabled(true);

                                HashMap<String, Object> combinedResult = new HashMap<>();
                                combinedResult.put("emotion_data", (Serializable) result);
                                combinedResult.put("feedback_data", (Serializable) feedback);
                                combinedResult.put("content_data", (Serializable) content);

                                Intent intent = new Intent(ActivityDaily.this, ActivityDailyResult.class);
                                intent.putExtra("ANALYSIS_RESULT", (Serializable) combinedResult);

                                startActivity(intent);
                            });
                        }
                        @Override
                        public void onError(Throwable throwable) {
                            mainHandler.post(() -> {
                                Toast.makeText(ActivityDaily.this, "피드백 불러오기에 실패하였습니다", Toast.LENGTH_SHORT).show();
                            });

                        }
                    };
                    geminiAI.generateFeedback(content, backgroundExecutor, feedbackCallback);
                }
                @Override
                public void onError(Throwable throwable) {
                    mainHandler.post(() -> {
                        Toast.makeText(ActivityDaily.this, "감정 분석에 실패하였습니다", Toast.LENGTH_SHORT).show();
                    });
                }
            };

            geminiAI.analyzeEmotion(content, backgroundExecutor, emotionCallback);
        });
    }

}
