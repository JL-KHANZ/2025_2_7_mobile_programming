package com.example.mobile_programming_2025_2;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobile_programming_2025_2.data.DailyEntry;

public class MainViewModel extends ViewModel {

    private final LocalRepository repository;
    private final MutableLiveData<String> mText;
    private final MutableLiveData<DailyEntry> todayEntryData = new MutableLiveData<>();
    public MainViewModel(LocalRepository repository) {
        this.repository = repository;
        this.mText = new MutableLiveData<>();
        this.mText.setValue("안녕하세요!");
    }

    public LiveData<String> getText() {
        return mText;
    }
    public void fetchTodayData() {
        todayEntryData.setValue(null);

        repository.getTodayEntry().addOnSuccessListener(dailyEntry -> {
            if (dailyEntry != null) {
                todayEntryData.setValue(dailyEntry);
                mText.setValue("오늘의 감정: " + dailyEntry.topEmotion);
            }
        }).addOnFailureListener(e -> {
            System.out.println("error getting today entry");
        });
    }
    public LiveData<DailyEntry> getTodayEntryData() {
        return todayEntryData;
    }
}
