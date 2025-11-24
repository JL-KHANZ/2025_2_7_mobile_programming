package com.example.mobile_programming_2025_2.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobile_programming_2025_2.LocalRepository;
import com.example.mobile_programming_2025_2.data.DailyEntry;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final LocalRepository repository;
    private final MutableLiveData<DailyEntry> todayEntryData = new MutableLiveData<>();

    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<DailyEntry> getTodayEntryData() {
        return todayEntryData;
    }
    public HomeViewModel(LocalRepository repository) {
        mText = new MutableLiveData<>();
        mText.setValue("안녕하세요!");
        this.repository = repository;
        fetchTodayData();
    }
    public void fetchTodayData() {
        todayEntryData.setValue(null);

        repository.getTodayEntry().addOnSuccessListener(dailyEntry -> {
            System.out.println("dailyEntry: " + dailyEntry.date);
            todayEntryData.setValue(dailyEntry);
        }).addOnFailureListener(e -> {
            // Handle failure
        });
    }



}