package com.example.mobile_programming_2025_2.ui;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import android.content.Context;
import androidx.annotation.NonNull;


import java.util.HashMap;
import java.util.Map;

import com.example.mobile_programming_2025_2.R;

public class ColorMapping {
    private static final Map<String, Integer> EMOTION_COLOR_RES = new HashMap<>();

    static {
        EMOTION_COLOR_RES.put("기쁨", R.color.joy_color);
        EMOTION_COLOR_RES.put("신뢰", R.color.trust_color);
        EMOTION_COLOR_RES.put("공포", R.color.fear_color);
        EMOTION_COLOR_RES.put("놀람", R.color.surprise_color);
        EMOTION_COLOR_RES.put("슬픔", R.color.sadness_color);
        EMOTION_COLOR_RES.put("혐오", R.color.disgust_color);
        EMOTION_COLOR_RES.put("분노", R.color.anger_color);
        EMOTION_COLOR_RES.put("기대", R.color.anticipation_color);

        EMOTION_COLOR_RES.put("희망", R.color.hope_color);
        EMOTION_COLOR_RES.put("죄책감", R.color.guilt_color);
        EMOTION_COLOR_RES.put("호기심", R.color.curiosity_color);
        EMOTION_COLOR_RES.put("절망", R.color.despair_color);
        EMOTION_COLOR_RES.put("불신", R.color.unbelief_color);
        EMOTION_COLOR_RES.put("선망", R.color.envy_color);
        EMOTION_COLOR_RES.put("냉소", R.color.cynicism_color);
        EMOTION_COLOR_RES.put("자부심", R.color.pride_color);

        EMOTION_COLOR_RES.put("염려", R.color.anxiety_color);
        EMOTION_COLOR_RES.put("큰기쁨", R.color.delight_color);
        EMOTION_COLOR_RES.put("감상적임", R.color.sentimentality_color);
        EMOTION_COLOR_RES.put("수치심", R.color.shame_color);
        EMOTION_COLOR_RES.put("격분", R.color.outrage_color);
        EMOTION_COLOR_RES.put("비관", R.color.pessimism_color);
        EMOTION_COLOR_RES.put("병리적 감정", R.color.morbidness_color);
        EMOTION_COLOR_RES.put("우월감", R.color.dominance_color);

        EMOTION_COLOR_RES.put("사랑", R.color.love_color);
        EMOTION_COLOR_RES.put("순종", R.color.submission_color);
        EMOTION_COLOR_RES.put("경외", R.color.awe_color);
        EMOTION_COLOR_RES.put("반감", R.color.disapproval_color);
        EMOTION_COLOR_RES.put("자책", R.color.remorse_color);
        EMOTION_COLOR_RES.put("경멸", R.color.contempt_color);
        EMOTION_COLOR_RES.put("공격성", R.color.aggressiveness_color);
        EMOTION_COLOR_RES.put("낙관", R.color.optimism_color);

    }

    private ColorMapping(){}

    @ColorInt
    public static int getEmotionColor(@NonNull Context context, String emotion) {
        if (emotion == null) {
            return ContextCompat.getColor(context, R.color.default_color);
        }

        String key = emotion;

        Integer resId = EMOTION_COLOR_RES.get(key);
        if (resId == null) {
            return ContextCompat.getColor(context, R.color.default_color);
        }

        return ContextCompat.getColor(context, resId);
    }

}
