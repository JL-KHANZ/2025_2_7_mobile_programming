package com.example.mobile_programming_2025_2.network;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class GeminiAITest {

    private static final String TAG = "GeminiAITest";

    @Test
    public void analyzeEmotion_whenApiCallIsSuccessful_receivesEmotionScores() throws InterruptedException {
        // Arrange
        final CountDownLatch latch = new CountDownLatch(1);
        // 결과 타입을 Map<String, Object>로 변경합니다.
        final Map<String, Object>[] resultHolder = new Map[1];
        final Throwable[] errorHolder = new Throwable[1];
        String testText = "오늘 날씨가 정말 좋아서 친구랑 공원에서 즐겁게 놀았어!";
        GeminiAI geminiAI = new GeminiAI();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Executor mainExecutor = appContext.getMainExecutor();

        // Act
        // 콜백의 onResponse 파라미터 타입을 Map<String, Object>로 변경합니다.
        geminiAI.analyzeEmotion(testText, mainExecutor, new GeminiAI.EmotionAnalysisCallback() {
            @Override
            public void onResponse(Map<String, Object> emotionScores) {
                Log.d(TAG, "[감정 분석] API 분석 성공: " + emotionScores);
                resultHolder[0] = emotionScores;
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "[감정 분석] API 호출 실패", throwable);
                errorHolder[0] = throwable;
                latch.countDown();
            }
        });

        // Assert
        boolean receivedResponse = latch.await(30, TimeUnit.SECONDS);
        assertThat(errorHolder[0]).isNull();
        assertThat(receivedResponse).isTrue();
        assertThat(resultHolder[0]).isNotNull();
        assertThat(resultHolder[0].isEmpty()).isFalse();
        // 주요 키가 포함되어 있는지 확인하는 방식으로 테스트를 개선합니다.
        assertThat(resultHolder[0].containsKey("기쁨")).isTrue();
        assertThat(resultHolder[0].containsKey("분석 감정")).isTrue();
        Log.i(TAG, "[감정 분석] 테스트 최종 통과! 결과: " + resultHolder[0]);
    }

    @Test
    public void generateFeedback_whenApiCallIsSuccessful_receivesFeedbackMap() throws InterruptedException {
        // Arrange
        final CountDownLatch latch = new CountDownLatch(1);
        // 결과 타입을 String[]에서 Map<String, Object>[]로 변경합니다.
        final Map<String, Object>[] resultHolder = new Map[1];
        final Throwable[] errorHolder = new Throwable[1];
        String testText = "요즘따라 일이 잘 안풀려서 너무 우울하고 힘들어.";
        GeminiAI geminiAI = new GeminiAI();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Executor mainExecutor = appContext.getMainExecutor();

        // Act
        // 콜백의 onResponse 파라미터 타입을 Map<String, Object>로 변경합니다.
        geminiAI.generateFeedback(testText, mainExecutor, new GeminiAI.FeedbackCallback() {
            @Override
            public void onResponse(Map<String, Object> feedbackMap) {
                Log.d(TAG, "[피드백 생성] API 분석 성공: " + feedbackMap);
                resultHolder[0] = feedbackMap;
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "[피드백 생성] API 호출 실패", throwable);
                errorHolder[0] = throwable;
                latch.countDown();
            }
        });

        // Assert
        boolean receivedResponse = latch.await(30, TimeUnit.SECONDS);
        assertThat(errorHolder[0]).isNull();
        assertThat(receivedResponse).isTrue();
        assertThat(resultHolder[0]).isNotNull();
        assertThat(resultHolder[0].isEmpty()).isFalse();
        // 주요 키가 포함되어 있는지 확인하는 방식으로 테스트를 개선합니다.
        assertThat(resultHolder[0].containsKey("공감")).isTrue();
        assertThat(resultHolder[0].containsKey("마인드셋 방법")).isTrue();
        Log.i(TAG, "[피드백 생성] 테스트 최종 통과! 결과: " + resultHolder[0]);
    }
}
