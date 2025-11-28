package com.example.mobile_programming_2025_2.ui.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.DailyEntry;
import com.example.mobile_programming_2025_2.ui.ColorMapping;
import com.example.mobile_programming_2025_2.ui.daily.ActivityDailyResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WidgetDailyEntry extends LinearLayout {

    private TextView topEmotion;
    private TextView empathy;
    private TextView mindset;
    private TextView feedback1;
    private TextView feedback2;
    private TextView content;
    private ColorMapping colorMapping = new ColorMapping();

    public WidgetDailyEntry(Context context) {
        super(context);
        init(context);
    }

    public WidgetDailyEntry(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WidgetDailyEntry(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_daily_entry, this, true);

        topEmotion = findViewById(R.id.widget_topEmotion);
        empathy = findViewById(R.id.widget_empathy);
        mindset = findViewById(R.id.widget_mindset);
        feedback1 = findViewById(R.id.widget_feedback_1);
        feedback2 = findViewById(R.id.widget_feedback_2);
        content = findViewById(R.id.widget_content);

        ImageButton toggleButton = findViewById(R.id.button_toggle_details);
        final LinearLayout detailsLayout = findViewById(R.id.details_layout);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailsLayout.getVisibility() == View.INVISIBLE) {
                    System.out.println("button clicked so now showing details");
                    detailsLayout.setVisibility(View.VISIBLE);
                     // toggleButton.setImageResource(R.drawable.arrow_up);
                } else {
                    detailsLayout.setVisibility(View.INVISIBLE);
                    // Optional: toggleButton.setImageResource(R.drawable.arrow_down);
                }
            }
        });
    }

    public void setDailyEntry(DailyEntry entry) {

        if (entry != null) {
            findViewById(R.id.widget_constraint_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.widget_placeholder).setVisibility(View.GONE);
            findViewById(R.id.details_layout).setVisibility(View.INVISIBLE);

            String entry_date = entry.date;
            String entry_topEmotion = entry.topEmotion;
            String entry_content = entry.content;

            Map<String, String> feedback = entry.feedback;
            Map<String, Integer> emotion = entry.emotions;

            displayEmotionResult(entry.emotions);
            displayFeedbackResults(entry.feedback);

            topEmotion.setText(entry_topEmotion);
            content.setText(entry_content);
            empathy.setText(feedback.get("공감"));
            mindset.setText(feedback.get("마인드셋 방법"));
            feedback1.setText(feedback.get("생활 루틴 1"));
            feedback2.setText(feedback.get("생활 루틴 2"));
        } else {
            findViewById(R.id.widget_constraint_layout).setVisibility(View.GONE);
            findViewById(R.id.widget_placeholder).setVisibility(View.VISIBLE);
            findViewById(R.id.details_layout).setVisibility(View.INVISIBLE);
        }
    }

    public void displayEmotionResult(Map<String, Integer> result) {
        if (result == null || result.isEmpty()) {
            System.out.println("result is null or empty");
            return;
        }

        LinearLayout uiResults = findViewById(R.id.widget_emotion_layout);
        TextView uiTopResult = findViewById(R.id.widget_topEmotion);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            String emotion = entry.getKey();
            Object score = entry.getValue();

            Number scoreNumb = (Number) score;
            float scoreFloat = scoreNumb.floatValue();
            View barItemView = inflater.inflate(R.layout.daily_result_emotion_bar, uiResults, false);

            TextView emotionLabel = barItemView.findViewById(R.id.emotion_label);
            View scoreBar = barItemView.findViewById(R.id.emotion_score_bar);
            View emptySpace = barItemView.findViewById(R.id.empty_space);
            TextView scoreText = barItemView.findViewById(R.id.emotion_score_text);

            emotionLabel.setText(emotion);
            scoreText.setText(String.format("%.1f", scoreFloat));

            // 각 감정별 색상 적용
            Integer emotionColor = colorMapping.getEmotionColor(this.getContext(), emotion);
            scoreBar.setBackgroundColor(emotionColor);

            // 10점 만점 기준으로 바 길이 조정
            LinearLayout.LayoutParams filledParams = (LinearLayout.LayoutParams) scoreBar.getLayoutParams();
            filledParams.width = 0;
            filledParams.weight = scoreFloat;
            scoreBar.setLayoutParams(filledParams);

            // 빈 공간 추가
            LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) emptySpace.getLayoutParams();
            emptyParams.width = 0;
            emptyParams.weight = 10.0f - scoreFloat;
            emptySpace.setLayoutParams(emptyParams);

            uiResults.addView(barItemView);
        }
    }

    private void displayFeedbackResults(Object result) {
        LinearLayout uiFeedbackResults = findViewById(R.id.widget_feedback_results_layout);

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

            TextView uiTitle = new TextView(this.getContext());
            TextView uiContent = new TextView(this.getContext());

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
}