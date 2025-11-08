package com.example.mobile_programming_2025_2.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class UserDiary {
    public String title;      // 일기 제목
    public String content;    // 일기 내용
    public String date;       // 작성 날짜 (yyyy-MM-dd)
    public String time;       // 작성 시간 (HH:mm)
    @ServerTimestamp public Date createdAt; // Firestore 서버 기준 자동 저장

    public UserDiary() {} // Firestore 역직렬화용

    public UserDiary(String title, String content, String date, String time) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.time = time;
    }
}

