package com.example.mobile_programming_2025_2.network;

import com.example.mobile_programming_2025_2.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;

public class GeminiAI {

    private final GenerativeModelFutures generativeModel;

    public interface ResponseCallback {
        void onResponse(String response);
        void onError(Throwable throwable);
    }


    public GeminiAI() {
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.GEMINI_API_KEY
        );
        this.generativeModel = GenerativeModelFutures.from(gm);
    }

    public void analyzeEmotion(String userText, Executor executor, EmotionAnalysisCallback callback) {
        String prompt = "다음 글의 감정을 로버트 플루치크의 8가지 기본 감정(분노, 공포, 슬픔, 혐오, 놀람, 기대, 신뢰, 기쁨)으로 각각 10점 만점으로 점수를 매겨줘. " +
                "결과는 반드시 JSON 형식으로 각 감정을 key로, 점수를 value로 반환해줘. 다른 부가적인 설명은 절대 추가하지 마. " +
                "예시: {\"분노\": 3, \"공포\": 1, \"슬픔\": 5, \"혐오\": 0, \"놀람\": 2, \"기대\": 6, \"신뢰\": 7, \"기쁨\": 8}. " +
                "분석할 글: \"" + userText + "\"";

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = generativeModel.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    // 모델이 응답 앞뒤에 추가할 수 있는 ```json ... ``` 같은 마크다운을 제거합니다.
                    String jsonString = result.getText().trim().replace("```json", "").replace("```", "");

                    JSONObject jsonObject = new JSONObject(jsonString);
                    Map<String, Integer> emotionScores = new HashMap<>();
                    Iterator<String> keys = jsonObject.keys();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        int value = jsonObject.getInt(key);
                        emotionScores.put(key, value);
                    }
                    callback.onResponse(emotionScores);
                } catch (JSONException e) {
                    callback.onError(new Exception("Failed to parse JSON response: " + result.getText(), e));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t);
            }
        }, executor);
    }
}
