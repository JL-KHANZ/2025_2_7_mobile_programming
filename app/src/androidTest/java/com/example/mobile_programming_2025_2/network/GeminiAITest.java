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

    // Logcat에서 필터링하기 위한 TAG를 추가합니다.
    private static final String TAG = "GeminiAITest";

    @Test
    public void analyzeEmotion_whenApiCallIsSuccessful_receivesEmotionScores() throws InterruptedException {
        // Arrange
        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, Integer>[] resultHolder = new Map[1];
        final Throwable[] errorHolder = new Throwable[1];
        String testText = "오늘 날씨가 정말 좋아서 친구랑 공원에서 즐겁게 놀았어!";
        GeminiAI geminiAI = new GeminiAI();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Executor mainExecutor = appContext.getMainExecutor();

        // Act
        geminiAI.analyzeEmotion(testText, mainExecutor, new GeminiAI.EmotionAnalysisCallback() {
            @Override
            public void onResponse(Map<String, Integer> emotionScores) {
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
        assertThat(resultHolder[0].containsKey("기쁨")).isTrue();
        Log.i(TAG, "[감정 분석] 테스트 최종 통과! 결과: " + resultHolder[0]);
    }

    @Test
    public void generateFeedback_whenApiCallIsSuccessful_receivesFeedbackString() throws InterruptedException {
        // Arrange
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] resultHolder = new String[1];
        final Throwable[] errorHolder = new Throwable[1];
        String testText = "요즘따라 일이 잘 안풀려서 너무 우울하고 힘들어."; // 피드백을 요청할 텍스트
        GeminiAI geminiAI = new GeminiAI();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Executor mainExecutor = appContext.getMainExecutor();

        // Act
        geminiAI.generateFeedback(testText, mainExecutor, new GeminiAI.FeedbackCallback() {
            @Override
            public void onResponse(String feedback) {
                Log.d(TAG, "[피드백 생성] API 분석 성공: " + feedback);
                resultHolder[0] = feedback;
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
        // 최종 결과를 로그로 출력합니다.
        Log.i(TAG, "[피드백 생성] 테스트 최종 통과! 결과: " + resultHolder[0]);
    }
}
