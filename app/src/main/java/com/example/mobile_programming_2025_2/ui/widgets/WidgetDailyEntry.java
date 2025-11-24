package com.example.mobile_programming_2025_2.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.DailyEntry;

import java.util.Map;

public class WidgetDailyEntry extends LinearLayout {

    private TextView topEmotion;
    private TextView empathy;
    private TextView mindset;
    private TextView feedback1;
    private TextView feedback2;
    private TextView content;

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
                if (detailsLayout.getVisibility() == View.GONE) {
                    detailsLayout.setVisibility(View.VISIBLE);
                     // toggleButton.setImageResource(R.drawable.arrow_up);
                } else {
                    detailsLayout.setVisibility(View.GONE);
                    // Optional: toggleButton.setImageResource(R.drawable.arrow_down);
                }
            }
        });
    }

    public void setDailyEntry(DailyEntry entry) {

        if (entry != null) {
            findViewById(R.id.widget_constraint_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.widget_placeholder).setVisibility(View.GONE);

            String entry_date = entry.date;
            String entry_topEmotion = entry.topEmotion;
            String entry_content = entry.content;

            Map<String, String> feedback = entry.feedback;
            Map<String, Integer> emotion = entry.emotions;

            System.out.println("got feedback data content: " + entry_content);

            topEmotion.setText(entry_topEmotion);
            content.setText(entry_content);
            empathy.setText(feedback.get("공감"));
            mindset.setText(feedback.get("마인드셋 방법"));
            feedback1.setText(feedback.get("생활 루틴 1"));
            feedback2.setText(feedback.get("생활 루틴 2"));
        } else {
            findViewById(R.id.widget_constraint_layout).setVisibility(View.GONE);
            findViewById(R.id.widget_placeholder).setVisibility(View.VISIBLE);
            findViewById(R.id.details_layout).setVisibility(View.GONE);
        }
    }
}