package com.example.mobile_programming_2025_2.network;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

/**
 * Instrumented test, which will execute on an Android device.
 * <p>
 * This test makes a REAL network request to the Gemini API to verify end-to-end functionality.
 * It requires a valid API key in local.properties and an active internet connection.
 */
@RunWith(AndroidJUnit4.class)
public class GeminiAITest {

    @Test
    public void analyzeEmotion_whenApiCallIsSuccessful_receivesEmotionScores() throws InterruptedException {
        // Arrange: 비동기 작업의 완료를 기다리기 위한 도구
        final CountDownLatch latch = new CountDownLatch(1);

        // 비동기 결과를 저장할 변수들
        final Map<String, Integer>[] resultHolder = new Map[1];
        final Throwable[] errorHolder = new Throwable[1];

        // 테스트할 텍스트
        String testText = "오늘 날씨가 정말 좋아서 친구랑 공원에서 즐겁게 놀았어!";

        // GeminiAI 인스턴스 생성
        GeminiAI geminiAI = new GeminiAI();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Executor mainExecutor = appContext.getMainExecutor();

        // Act: 감정 분석 API 호출
        geminiAI.analyzeEmotion(testText, mainExecutor, new GeminiAI.EmotionAnalysisCallback() {
            @Override
            public void onResponse(Map<String, Integer> emotionScores) {
                // 성공 시 결과를 저장하고 대기 종료
                resultHolder[0] = emotionScores;
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                // 실패 시 에러를 저장하고 콘솔에 전체 오류를 출력합니다.
                System.err.println("API call failed inside test:");
                throwable.printStackTrace();
                errorHolder[0] = throwable;
                latch.countDown();
            }
        });

        // Assert: API 응답이 올 때까지 최대 30초간 대기 (시간 연장)
        boolean receivedResponse = latch.await(30, TimeUnit.SECONDS);

        // 최종 결과 검증
        assertThat(errorHolder[0]).isNull(); // API 에러가 없었는지 먼저 확인
        assertThat(receivedResponse).isTrue(); // 응답이 시간 내에 도착했는지 확인
        assertThat(resultHolder[0]).isNotNull(); // 성공 결과가 null이 아닌지 확인
        assertThat(resultHolder[0].isEmpty()).isFalse(); // 결과 맵이 비어있지 않은지 확인
        assertThat(resultHolder[0].containsKey("기쁨")).isTrue(); // 결과에 '기쁨' 점수가 포함되어 있는지 확인

        // 결과 출력
        System.out.println("API 분석 결과: " + resultHolder[0]);
    }
}
