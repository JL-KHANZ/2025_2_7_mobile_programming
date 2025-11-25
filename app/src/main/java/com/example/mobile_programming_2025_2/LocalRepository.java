package com.example.mobile_programming_2025_2;

import androidx.annotation.NonNull;

import com.example.mobile_programming_2025_2.Service.DailyEntryService;
import com.example.mobile_programming_2025_2.data.DailyEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalRepository {
    private final DailyEntryService dailyEntryService;
    private static String today;
    public LocalRepository(DailyEntryService dailyEntryService) {
        this.dailyEntryService = dailyEntryService;
    }
    public void setDate() {
        today = getTodayDateString();
        dailyEntryService.setDate(today);
        System.out.println("date set to " + today);
    }
    public String getTodayDateString() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = dateFormat.format(calendar.getTime());
        return date;
    }

    // --- Upload Daily Entry ---
    public Task<Void> upsertTodayContent(@NonNull String content) {
        System.out.println("upsertTodayContent called with content: " + content);
        return dailyEntryService.upsertTodayContent(content);
    }
    public Task<Void> upsertTodayEmotion(@NonNull Map<String, Object> emotions) {
        System.out.println("upsertTodayEmotion called with emotions: " + emotions);
        String topEmotion = "";
        Map<String, Integer> convertedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : emotions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                topEmotion = (String) value;
            } else if (value instanceof Number) {
                convertedMap.put(key, ((Number) value).intValue());
            }
        }
        return dailyEntryService.upsertTodayEmotion(convertedMap, topEmotion);
    }
    public Task<Void> upsertTodayFeedback(@NonNull Object feedback) {
        System.out.println("upsertTodayFeedback called with feedback: " + feedback);
        Map<String, String> feedbackMap = (Map<String, String>) feedback;
        return dailyEntryService.upsertTodayFeedback(feedbackMap);
    }

    // --- Get Daily Entry ---
    public Task<DailyEntry> getTodayEntry() {
        System.out.println("getTodayEntry called");
        final TaskCompletionSource<DailyEntry> tcs = new TaskCompletionSource<>();

        String date = getTodayDateString();
        dailyEntryService.getByPeriod(date, date,
                dailyEntry -> {
                    DailyEntry todayEntry = null;
                    if (dailyEntry != null) {
                        todayEntry = dailyEntry.get(date);
                    }
                    tcs.setResult(todayEntry);
                },
                e -> {
                    tcs.setException(e);
                }
        );
        return tcs.getTask();
    }
    public Task<DailyEntry> getEntries(String startdate, String enddate) {
        System.out.println("getTodayEntry called");
        final TaskCompletionSource<DailyEntry> tcs = new TaskCompletionSource<>();

        String date = getTodayDateString();
        dailyEntryService.getByPeriod(startdate, enddate,
                dailyEntry -> {
                    DailyEntry todayEntry = null;
                    if (dailyEntry != null) {
                        todayEntry = dailyEntry.get(date);
                    }
                    tcs.setResult(todayEntry);
                },
                e -> {
                    tcs.setException(e);
                }
        );
        return tcs.getTask();
    }
}