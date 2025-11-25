package com.example.mobile_programming_2025_2.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_programming_2025_2.LocalRepository;

// HomeViewModelFactory.java
public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final LocalRepository repository;

    public HomeViewModelFactory(LocalRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            // Unchecked cast is suppressed because we checked the class type
            return (T) new HomeViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}