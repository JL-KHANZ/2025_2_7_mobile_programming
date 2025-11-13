package com.example.mobile_programming_2025_2.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import java.util.Date;
import java.util.List;
import java.util.Map;

class DailyEntry {
    private String content;
    private Date date;
    private Map<String, Integer> emotion;
    private String topEmotion;
    private List<String> feedback;

    // Constructor
    public DailyEntry(String content, Date date, Map<String, Integer> emotion,
                      String topEmotion, List<String> feedback) {
        this.content = content;
        this.date = date;
        this.emotion = emotion;
        this.topEmotion = topEmotion;
        this.feedback = feedback;
    }
}



public class TestService {

    List<DailyEntry> getCalendarData(Date date, Callback calendarCallback) {
        List<DailyEntry> monthlyEntry = new ArrayList<>();

        for (int i = 1; i <= 30; i++) {
            DailyEntry entry = new DailyEntry(
                    "일기 내용 " + i,
                    new Date(), // you can adjust the date per entry if you want
                    Map.of("감정1", i * 3, "감정2", 100 - i * 3),
                    "감정1",
                    List.of("피드백1", "피드백2")
            );
            monthlyEntry.add(entry);
        }
        return monthlyEntry;
    }
}
