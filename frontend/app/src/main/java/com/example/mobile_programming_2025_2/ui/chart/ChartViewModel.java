package com.example.mobile_programming_2025_2.ui.chart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ChartViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ChartViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is chart fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}