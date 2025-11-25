package com.example.mobile_programming_2025_2.ui.chat;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.mobile_programming_2025_2.ui.chat.ChatRepository;

public class ChatViewModelFactory implements ViewModelProvider.Factory {

    private final ChatRepository repository;
    public ChatViewModelFactory(ChatRepository repository) {
        this.repository = repository;
    }

    /**
     * This method is called by the ViewModelProvider to create the ViewModel instance.
     * @param modelClass The class of the ViewModel requested (should be ChatViewModel.class).
     * @return A new instance of the ChatViewModel.
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

        if (modelClass.isAssignableFrom(ChatViewModel.class)) {

            try {
                return (T) new ChatViewModel(repository);
            } catch (Exception e) {
                throw new RuntimeException("Cannot create an instance of ChatViewModel", e);
            }
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}