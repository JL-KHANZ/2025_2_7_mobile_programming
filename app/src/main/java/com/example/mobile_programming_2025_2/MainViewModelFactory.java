package com.example.mobile_programming_2025_2;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

// This factory is necessary because MainViewModel has a constructor argument (LocalRepository)
public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final LocalRepository repository;

    public MainViewModelFactory(LocalRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            try {
                return (T) new MainViewModel(repository);
            } catch (Exception e) {
                throw new RuntimeException("Error creating MainViewModel: " + e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}