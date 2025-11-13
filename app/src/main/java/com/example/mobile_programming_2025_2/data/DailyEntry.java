package com.example.mobile_programming_2025_2.data;
/**
 * 역직렬화용 객체,
 *형식을 위한 파일
 */
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Map;
public class DailyEntry {
    public String date;                     // yyyy-MM-dd (문서ID와 동일)
    public String content;                  // 일기 본문
    public Map<String, String> feedback;     // AI피드백   {wormWord:String, mindset:String ,feedback1:String,feedback2:String }
    public Map<String, Integer> emotions;   // 감정 점수 맵 { "분노":3, "기쁨":8, ... }
    public String topEmotion;               // 메인 감정


    @ServerTimestamp
    public Date createdAt; // 최초 생성 시 자동
    @ServerTimestamp
    public Date updatedAt; // 매 저장 시 자동

    public DailyEntry() {
    } // Firestore 역직렬화용
        public DailyEntry(String date, Map<String,String> feedback,
                      Map<String, Integer> emotions, String topEmotion) {
        this.date = date;
        this.content = content;
        this.feedback = feedback;
        this.emotions = emotions;
        this.topEmotion = topEmotion;
    }
//    //일기
//    public DailyEntry(String date, String content) {
//        this.content = content;
//        this.date = date;
//    }
//
//    // 피드백{wormWord:String, mindset:String ,feedback1:String,feedback2:String }
//    public DailyEntry(Map<String, String> feedback) {
//        this.feedback = feedback;
//    }
//
//    //감정
//    public DailyEntry(Map<String, Integer> emotions, String topEmotion) {
//        this.emotions = emotions;
//        this.topEmotion = topEmotion;
//    }
}



