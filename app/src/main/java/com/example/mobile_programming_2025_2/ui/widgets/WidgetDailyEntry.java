package com.example.mobile_programming_2025_2.ui.widgets;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.DailyEntry;
import com.example.mobile_programming_2025_2.ui.ColorMapping;

import java.util.Map;

public class WidgetDailyEntry extends LinearLayout {

    private TextView topEmotion;
    private TextView empathy;
    private TextView mindset;
    private TextView content;
    private TextView placeholderText;

    private View widgetConstraintLayout;
    private View widgetPlaceholder;
    private LinearLayout detailsLayout;
    private ImageButton toggleButton;

    private int layoutResId = R.layout.widget_daily_entry;

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

    public WidgetDailyEntry(Context context, int layoutResId) {
        super(context);
        this.layoutResId = layoutResId;
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(this.layoutResId, this, true);

        topEmotion = findViewById(R.id.widget_topEmotion);
        empathy = findViewById(R.id.widget_empathy);
        mindset = findViewById(R.id.widget_mindset);
        content = findViewById(R.id.widget_content);
        placeholderText = findViewById(R.id.widget_placeholder_text);

        widgetConstraintLayout = findViewById(R.id.widget_constraint_layout);
        widgetPlaceholder = findViewById(R.id.widget_placeholder);
        detailsLayout = findViewById(R.id.details_layout);
        toggleButton = findViewById(R.id.button_toggle_details);

        if (toggleButton != null) {
            toggleButton.setOnClickListener(v -> {
                if (detailsLayout.getVisibility() == View.GONE) {
                    detailsLayout.setVisibility(View.VISIBLE);
                    toggleButton.setImageResource(android.R.drawable.arrow_up_float);
                } else {
                    detailsLayout.setVisibility(View.GONE);
                    toggleButton.setImageResource(android.R.drawable.arrow_down_float);
                }
            });
        }
    }

    public void setDailyEntry(DailyEntry entry, String dateString) {
        if (entry != null) {
            widgetConstraintLayout.setVisibility(View.VISIBLE);
            widgetPlaceholder.setVisibility(View.GONE);
            if(detailsLayout != null) detailsLayout.setVisibility(View.GONE);
            if(toggleButton != null) toggleButton.setImageResource(android.R.drawable.arrow_down_float);

            String entry_topEmotion = entry.topEmotion;
            String entry_content = entry.content;
            Map<String, String> feedback = entry.feedback;

            // ⭐️ [수정됨] 레이아웃 종류에 따라 제목 형식 다르게!
            String fullTitle;
            if (this.layoutResId == R.layout.widget_daily_entry) {
                // 홈 화면: 줄바꿈
                fullTitle = "오늘의 감정\n" + entry_topEmotion;
            } else {
                // 캘린더: 날짜 포함 한 줄
                fullTitle = dateString + "의 감정 : " + entry_topEmotion;
            }

            SpannableString spannableTitle = new SpannableString(fullTitle);
            int emotionColor = ColorMapping.getEmotionColor(getContext(), entry_topEmotion);
            int startIndex = fullTitle.lastIndexOf(entry_topEmotion);

            if (startIndex != -1) {
                spannableTitle.setSpan(
                        new ForegroundColorSpan(emotionColor),
                        startIndex,
                        startIndex + entry_topEmotion.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            topEmotion.setText(spannableTitle);

            content.setText(entry_content);
            if (feedback != null) {
                if (empathy != null) empathy.setText(feedback.get("공감"));
                mindset.setText(feedback.get("마인드셋 방법"));
            }

        } else {
            widgetConstraintLayout.setVisibility(View.GONE);
            widgetPlaceholder.setVisibility(View.VISIBLE);

            String message = dateString + " :\n감정 기록이 없습니다";
            if (placeholderText != null) placeholderText.setText(message);
        }
    }
}