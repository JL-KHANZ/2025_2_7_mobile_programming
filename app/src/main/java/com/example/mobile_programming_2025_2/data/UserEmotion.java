package com.example.mobile_programming_2025_2.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Map;

/**
 * Firestore 구조:
 * {
 *   "date": "2025-11-09",
 *   "emotions": { "분노":3, "기쁨":8, ... },
 *   "top1": { "기쁨": 8 },
 *   "top2": { "슬픔": 5 },
 *   "createdAt": <Timestamp>,
 *   "updatedAt": <Timestamp>
 * }
 */
public class UserEmotion {

    public String date;                       // yyyy-MM-dd
    public Map<String, Integer> emotions;     // 전체 감정 점수 Map
    public Map<String, Integer> top1;         // 상위 1개 감정 { "기쁨": 8 }
    public Map<String, Integer> top2;         // 상위 2개 감정 { "슬픔": 5 }

    @ServerTimestamp public Date createdAt;   // Firestore 자동 생성
    @ServerTimestamp public Date updatedAt;   // Firestore 자동 갱신

    public UserEmotion() {} // Firestore 역직렬화용 기본 생성자

    public UserEmotion(String date,
                       Map<String, Integer> emotions,
                       Map<String, Integer> top1,
                       Map<String, Integer> top2) {
        this.date = date;
        this.emotions = emotions;
        this.top1 = top1;
        this.top2 = top2;
    }
}
