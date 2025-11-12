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

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.databinding.ActivityDaily1SliderBinding;
import com.example.mobile_programming_2025_2.network.GeminiAI.EmotionAnalysisCallback;
import com.example.mobile_programming_2025_2.network.GeminiAI;
import com.example.mobile_programming_2025_2.ui.CircularSliderView;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ActivityDaily extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_1_slider);

        // 감정 슬라이더 일단 주석처리 - 1차 기능 아니라 제외
//        CircularSliderView slider = findViewById(R.id.circleSlider);
//        slider.setOnValueChangeListener(value -> {
//            // value is 0..100
//            // e.g., update a TextView, send to ViewModel, etc.
//            System.out.println("value => " + value);
//        });
//
//        ImageButton btnArrow = findViewById(R.id.daily_1_btn_2);
//        btnArrow.setOnClickListener(v -> {
//            setContentView(R.layout.activity_daily_2_journal);
//
//            journalLayoutViews();
//        });
        journalLayoutViews();
    }

    private void journalLayoutViews() {
        setContentView(R.layout.activity_daily_2_journal);
        Button btnJournal = findViewById(R.id.daily_journal_btn);

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
        final GeminiAI geminiAI = new GeminiAI();

        btnJournal.setOnClickListener(v -> {
            setContentView(R.layout.activity_daily_2_journal);
            EditText titleText = findViewById(R.id.daily_input_title);
            EditText contentText = findViewById(R.id.daily_input_content);
            String title = titleText.getText().toString();
            String content = contentText.getText().toString();

            EmotionAnalysisCallback callback = new EmotionAnalysisCallback() {
                @Override
                public void onResponse(Map<String, Integer> result) {
                    mainHandler.post(() -> {
                        StringBuilder uiMessage = new StringBuilder();
                        String topEmotion = "N/A";
                        int highestScore = -1;

                        for (Map.Entry<String, Integer> entry : result.entrySet()) {
                            String emotion = entry.getKey();
                            int score = entry.getValue();
                            uiMessage.append(emotion).append(": ").append(score).append("\n");
                            if (score > highestScore) {
                                highestScore = score;
                                topEmotion = emotion;
                            }
                        }

                        // show top result
                        TextView uiResult = findViewById(R.id.daily_result_text);
                        uiResult.setText(topEmotion);

                        // show all results
                        LinearLayout uiResults = findViewById(R.id.daily_results_layout);
                        uiResults.removeAllViews();
                        LayoutInflater inflater = getLayoutInflater();

                        for (Map.Entry<String, Integer> entry : result.entrySet()) {
                            String emotion = entry.getKey();
                            int score = entry.getValue();

                            View barItemView = inflater.inflate(R.layout.daily_result_emotion_bar, uiResults, false);

                            TextView emotionLabel = barItemView.findViewById(R.id.emotion_label);
                            View scoreBar = barItemView.findViewById(R.id.emotion_score_bar);
                            TextView scoreText = barItemView.findViewById(R.id.emotion_score_text);

                            emotionLabel.setText(emotion);
                            scoreBar.getLayoutParams().width = (int) (score * 10);
                            scoreText.setText(score + "%");

                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scoreBar.getLayoutParams();
                            params.width = 0;
                            params.weight = score;
                            scoreBar.setLayoutParams(params);

                            uiResults.addView(barItemView);
                        }



                        // send to db here
                    });
                }
                @Override
                public void onError(Throwable throwable) {
                    mainHandler.post(() -> {
                        Toast.makeText(ActivityDaily.this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    });
                }

            };

            geminiAI.analyzeEmotion(content, backgroundExecutor, callback);
            setContentView(R.layout.activity_daily_3_result);
            resultLayoutViews();
        });
    }

    private void resultLayoutViews() {
        setContentView(R.layout.activity_daily_3_result);

    }

}