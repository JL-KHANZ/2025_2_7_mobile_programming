package com.example.mobile_programming_2025_2.network;

import java.util.Map;

public interface EmotionAnalysisCallback {
    void onResponse(Map<String, Integer> emotionScores);
    void onError(Throwable throwable);
}