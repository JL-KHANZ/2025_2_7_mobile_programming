package com.example.mobile_programming_2025_2.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap; // TreeMap을 사용하여 키(key)를 기준으로 자동 정렬합니다.
import java.util.concurrent.Executor;

public class GeminiAI {

    private static final String TAG = "GeminiAI";
    private final GenerativeModelFutures generativeModel;
    private static final int MAX_RETRIES = 3; // 최대 재시도 횟수
    private static final long INITIAL_BACKOFF_MS = 2000; // 초기 대기 시간 (2초)

    // --- 콜백 인터페이스들 ---
    public interface FeedbackCallback {
        void onResponse(Map<String, Object> feedbackMap);
        void onError(Throwable throwable);
    }

    public interface EmotionAnalysisCallback {
        void onResponse(Map<String, Object> emotionScores);
        void onError(Throwable throwable);
    }

    public GeminiAI() {
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        this.generativeModel = GenerativeModelFutures.from(gm);
    }

    // --- 공개 API 메소드들 ---
    public void generateFeedback(String userText, Executor executor, FeedbackCallback callback) {
        String prompt = "다음 글의 감정을 공감하고 개선할 수 있는 마인드셋 방법 1가지와 생활 루틴 2개를 추천해줘. " + "결과는 반드시 JSON 형식으로 '공감', '마인드셋 방법', '생활 루틴 1', '생활 루틴 2'를 key로, 각각에 해당하는 내용을 value로 반환해줘. 다른 부가적인 설명은 절대 추가하지 마. " + "예시: {\\\"공감\\\": \\\"...\\\", \\\"마인드셋 방법\\\": \\\"...\\\", \\\"생활 루틴 1\\\": \\\"...\\\", \\\"생활 루틴 2\\\": \\\"...\\\"}. " + "분석할 글: \"" + userText + "\"";
        Content content = new Content.Builder().addText(prompt).build();

        executeWithRetry(content, executor, 0, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    Map<String, Object> resultMap = parseJsonObject(result.getText());
                    callback.onResponse(resultMap);
                } catch (JSONException e) {
                    callback.onError(new Exception("Failed to parse JSON response for feedback: " + result.getText(), e));
                }
            }
            @Override
            public void onFailure(Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void analyzeEmotion(String userText, Executor executor, EmotionAnalysisCallback callback) {
        String prompt = "다음 글의 감정을 로버트 플루치크의 8가지 기본 감정(분노, 공포, 슬픔, 혐오, 놀람, 기대, 신뢰, 기쁨)으로 각각 10점 만점으로 점수를 매겨줘.동점이 안 나오게 소수점까지 점수를 매겨줘." +
                "감정 점수 상위 2개의 감정들을 조합해서 파생된 분석 감정을 알려줘. 만약에 상위 2개의 감정들끼리 5점 이상 차이가 난다면 최상위 점수 감정이 분석된 감정이야. 조합식 : 기쁨+신뢰=사랑, 기쁨+공포=죄책감, 기쁨+놀람=큰기쁨, 기쁨+혐오=병리적 감정, 기쁨+분노=자부심, 기쁨+기대=낙관, 신뢰+공포=순종(굴복), 신뢰+놀람=호기심, 신뢰+슬픔=감상적임, 신뢰+분노=우월감, 신뢰+기대=희망, 공포+놀람=경외, 공포+슬픔=절망, 공포+혐오=수치심, 공포+기대=염려, 놀람+슬픔=반감, 놀람+혐오=불신, 놀람+분노=격분, 슬픔+혐오=자책(회한), 슬픔+분노=선망(부러움), 슬픔+기대=비관, 혐오+분노=경멸, 혐오+기대=냉소, 분노+기대=공격성" +
                "결과는 반드시 JSON 형식으로 각 감정을 key로, 점수를 value로 반환해줘. 다른 부가적인 설명은 절대 추가하지 마. " +
                "예시: {\\\"분노\\\": 1.2, \\\"공포\\\": 4.5, \\\"슬픔\\\": 9.8, \\\"혐오\\\": 3.1, \\\"놀람\\\": 1.8, \\\"기대\\\": 0.5, \\\"신뢰\\\": 0.1, \\\"기쁨\\\": 0.0, \\\"분석 감정\\\": 절망}. " +
                "분석할 글: \"" + userText + "\"";
        Content content = new Content.Builder().addText(prompt).build();

        executeWithRetry(content, executor, 0, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    Map<String, Object> resultMap = parseJsonObject(result.getText());
                    callback.onResponse(resultMap);
                } catch (JSONException e) {
                    callback.onError(new Exception("Failed to parse JSON response for emotion: " + result.getText(), e));
                }
            }
            @Override
            public void onFailure(Throwable t) {
                callback.onError(t);
            }
        });
    }

    // --- 재시도 및 JSON 파싱을 위한 내부 헬퍼 메소드들 ---

    private void executeWithRetry(Content content, Executor executor, int attempt, FutureCallback<GenerateContentResponse> finalCallback) {
        ListenableFuture<GenerateContentResponse> response = generativeModel.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                finalCallback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                if (isRetryable(t) && attempt < MAX_RETRIES) {
                    long delay = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt);
                    Log.w(TAG, "API call failed, retrying in " + delay + "ms (Attempt #" + (attempt + 1) + ")", t);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        executeWithRetry(content, executor, attempt + 1, finalCallback);
                    }, delay);
                } else {
                    Log.e(TAG, "API call failed after " + (attempt + 1) + " retries.", t);
                    finalCallback.onFailure(t);
                }
            }
        }, executor);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ServerException) {
            String errorMessage = throwable.getMessage();
            return errorMessage != null && errorMessage.contains("\"code\": 503");
        }
        return false;
    }

    private Map<String, Object> parseJsonObject(String jsonText) throws JSONException {
        String cleanJson = jsonText.trim().replace("```json", "").replace("```", "");
        JSONObject jsonObject = new JSONObject(cleanJson);
        // HashMap 대신 TreeMap을 사용하여 키를 자동으로 정렬합니다.
        Map<String, Object> resultMap = new TreeMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            resultMap.put(key, value);
        }
        return resultMap;
    }
}
