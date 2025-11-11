package com.example.mobile_programming_2025_2.ui.daily;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.databinding.ActivityDaily1SliderBinding;
import com.example.mobile_programming_2025_2.network.EmotionAnalysisCallback;
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

        CircularSliderView slider = findViewById(R.id.circleSlider);
        slider.setOnValueChangeListener(value -> {
            // value is 0..100
            // e.g., update a TextView, send to ViewModel, etc.
            System.out.println("value => " + value);
        });

        ImageButton btnArrow = findViewById(R.id.daily_1_btn_2);
        btnArrow.setOnClickListener(v -> {
            setContentView(R.layout.activity_daily_2_journal);

            journalLayoutViews();
        });


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

                        TextView uiResult = findViewById(R.id.daily_result_text); // send to daily 3
                        uiResult.setText(uiMessage.toString());

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