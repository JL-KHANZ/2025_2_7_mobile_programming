package com.example.mobile_programming_2025_2.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mobile_programming_2025_2.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.ServerException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiAI {

    private static final String TAG = "GeminiAI";
    private final GenerativeModelFutures generativeModel;
    private static final int MAX_RETRIES = 3; // 최대 재시도 횟수
    private static final long INITIAL_BACKOFF_MS = 2000; // 초기 대기 시간 (2초)

    public interface FeedbackCallback {
        void onResponse(@NonNull Map<String, Object> feedbackMap);
        void onError(@NonNull Throwable throwable);
    }

    public interface EmotionAnalysisCallback {
        void onResponse(@NonNull Map<String, Object> emotionScores);
        void onError(@NonNull Throwable throwable);
    }

    public GeminiAI() {
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash", // 안정성을 위해 최신 모델 사용
                BuildConfig.GEMINI_API_KEY
        );
        this.generativeModel = GenerativeModelFutures.from(gm);
    }

    public void generateFeedback(String userText, Executor executor, FeedbackCallback callback) {
        String prompt = "다음 사용자의 글에 대해 따뜻한 공감의 메시지 한 개, 실천 가능한 마인드셋 조언 한 개, 그리고 간단한 생활 루틴 두 개를 추천해줘. " +
                "결과는 반드시 JSON 형식으로만 응답해야 하며, 다른 부가적인 설명은 절대 추가하지 마. " +
                "JSON의 키는 \"공감\", \"마인드셋 방법\", \"생활 루틴 1\", \"생활 루틴 2\"를 사용해야 해. " +
                "분석할 글: \"" + userText + "\"";

        Content content = new Content.Builder().addText(prompt).build();

        executeWithRetry(content, executor, 0, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    Map<String, Object> resultMap = parseJsonObject(result.getText());
                    callback.onResponse(resultMap);
                } catch (JSONException e) {
                    callback.onError(new Exception("Failed to parse feedback JSON: " + result.getText(), e));
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void analyzeEmotion(String userText, Executor executor, EmotionAnalysisCallback callback) {
        String prompt = "다음 글의 감정을 로버트 플루치크의 8가지 기본 감정(기쁨, 신뢰, 공포, 놀람, 슬픔, 혐오, 분노, 기대)으로 각각 10점 만점의 소수점 첫째 자리까지의 점수로 평가해줘. " +
                "결과는 반드시 8개의 감정 키와 점수만 포함된 JSON 형식으로만 응답해야 하며, 다른 부가적인 설명은 절대 추가하지 마. " +
                "분석할 글: \"" + userText + "\"";

        Content content = new Content.Builder().addText(prompt).build();

        executeWithRetry(content, executor, 0, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    Map<String, Object> resultMap = parseAndCalculateAnalyzedEmotion(result.getText());
                    callback.onResponse(resultMap);
                } catch (Exception e) {
                    callback.onError(new Exception("Failed to parse or process emotion JSON: " + result.getText(), e));
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    private void executeWithRetry(Content content, Executor executor, int attempt, FutureCallback<GenerateContentResponse> finalCallback) {
        ListenableFuture<GenerateContentResponse> response = generativeModel.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                finalCallback.onSuccess(result);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (t instanceof ServerException && attempt < MAX_RETRIES) {
                    long delay = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt);
                    Log.w(TAG, "API call failed, retrying in " + delay + "ms (Attempt #" + (attempt + 1) + ")", t);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> executeWithRetry(content, executor, attempt + 1, finalCallback), delay);
                } else {
                    Log.e(TAG, "API call failed after " + (attempt + 1) + " retries.", t);
                    finalCallback.onFailure(t);
                }
            }
        }, executor);
    }

    private String extractJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "{}";
        }

        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7; // "```json".length()
            int end = text.lastIndexOf("```");
            if (end > start) {
                text = text.substring(start, end);
            }
        }

        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim();
        }

        return "{}";
    }

    private Map<String, Object> parseJsonObject(String rawText) throws JSONException {
        String jsonText = extractJson(rawText);
        JSONObject jsonObject = new JSONObject(jsonText);
        Map<String, Object> resultMap = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            resultMap.put(key, value);
        }
        return resultMap;
    }

    private Map<String, Object> parseAndCalculateAnalyzedEmotion(String rawText) throws JSONException {
        Map<String, Object> resultMap = parseJsonObject(rawText);

        Map.Entry<String, Double> top1 = null, top2 = null;

        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
            try {
                double score = ((Number) entry.getValue()).doubleValue();
                if (top1 == null || score > top1.getValue()) {
                    top2 = top1;
                    top1 = new AbstractMap.SimpleEntry<>(entry.getKey(), score);
                } else if (top2 == null || score > top2.getValue()) {
                    top2 = new AbstractMap.SimpleEntry<>(entry.getKey(), score);
                }
            } catch (ClassCastException e) {
                Log.w(TAG, "'" + entry.getKey() + "' is not a number, skipping.");
            }
        }

        if (top1 != null) {
            String analyzedEmotion = top1.getKey();
            if (top2 != null && Math.abs(top1.getValue() - top2.getValue()) < 3.0) {
                analyzedEmotion = getCombinedEmotion(top1.getKey(), top2.getKey());
                if (analyzedEmotion == null) analyzedEmotion = top1.getKey(); 
            }
            resultMap.put("분석 감정", analyzedEmotion);
        }

        return resultMap;
    }

    private String getCombinedEmotion(String emotion1, String emotion2) {
        if ((emotion1.equals("기쁨") && emotion2.equals("신뢰")) || (emotion1.equals("신뢰") && emotion2.equals("기쁨"))) return "사랑";
        if ((emotion1.equals("기쁨") && emotion2.equals("공포")) || (emotion1.equals("공포") && emotion2.equals("기쁨"))) return "죄책감";
        if ((emotion1.equals("기쁨") && emotion2.equals("놀람")) || (emotion1.equals("놀람") && emotion2.equals("기쁨"))) return "큰기쁨";
        if ((emotion1.equals("기쁨") && emotion2.equals("혐오")) || (emotion1.equals("혐오") && emotion2.equals("기쁨"))) return "병리적 감정";
        if ((emotion1.equals("기쁨") && emotion2.equals("분노")) || (emotion1.equals("분노") && emotion2.equals("기쁨"))) return "자부심";
        if ((emotion1.equals("기쁨") && emotion2.equals("기대")) || (emotion1.equals("기대") && emotion2.equals("기쁨"))) return "낙관";
        if ((emotion1.equals("신뢰") && emotion2.equals("공포")) || (emotion1.equals("공포") && emotion2.equals("신뢰"))) return "순종";
        if ((emotion1.equals("신뢰") && emotion2.equals("놀람")) || (emotion1.equals("놀람") && emotion2.equals("신뢰"))) return "호기심";
        if ((emotion1.equals("신뢰") && emotion2.equals("슬픔")) || (emotion1.equals("슬픔") && emotion2.equals("신뢰"))) return "감상적임";
        if ((emotion1.equals("신뢰") && emotion2.equals("분노")) || (emotion1.equals("분노") && emotion2.equals("신뢰"))) return "우월감";
        if ((emotion1.equals("신뢰") && emotion2.equals("기대")) || (emotion1.equals("기대") && emotion2.equals("신뢰"))) return "희망";
        if ((emotion1.equals("공포") && emotion2.equals("놀람")) || (emotion1.equals("놀람") && emotion2.equals("공포"))) return "경외";
        if ((emotion1.equals("공포") && emotion2.equals("슬픔")) || (emotion1.equals("슬픔") && emotion2.equals("공포"))) return "절망";
        if ((emotion1.equals("공포") && emotion2.equals("혐오")) || (emotion1.equals("혐오") && emotion2.equals("공포"))) return "수치심";
        if ((emotion1.equals("공포") && emotion2.equals("기대")) || (emotion1.equals("기대") && emotion2.equals("공포"))) return "염려";
        if ((emotion1.equals("놀람") && emotion2.equals("슬픔")) || (emotion1.equals("슬픔") && emotion2.equals("놀람"))) return "반감";
        if ((emotion1.equals("놀람") && emotion2.equals("혐오")) || (emotion1.equals("혐오") && emotion2.equals("놀람"))) return "불신";
        if ((emotion1.equals("놀람") && emotion2.equals("분노")) || (emotion1.equals("분노") && emotion2.equals("놀람"))) return "격분";
        if ((emotion1.equals("슬픔") && emotion2.equals("혐오")) || (emotion1.equals("혐오") && emotion2.equals("슬픔"))) return "자책";
        if ((emotion1.equals("슬픔") && emotion2.equals("분노")) || (emotion1.equals("분노") && emotion2.equals("슬픔"))) return "선망";
        if ((emotion1.equals("슬픔") && emotion2.equals("기대")) || (emotion1.equals("기대") && emotion2.equals("슬픔"))) return "비관";
        if ((emotion1.equals("혐오") && emotion2.equals("분노")) || (emotion1.equals("분노") && emotion2.equals("혐오"))) return "경멸";
        if ((emotion1.equals("혐오") && emotion2.equals("기대")) || (emotion1.equals("기대") && emotion2.equals("혐오"))) return "냉소";
        if ((emotion1.equals("분노") && emotion2.equals("기대")) || (emotion1.equals("기대") && emotion2.equals("분노"))) return "공격성";
        return null; // 조합이 없는 경우
    }
}
