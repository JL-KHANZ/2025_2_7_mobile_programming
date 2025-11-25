package com.example.mobile_programming_2025_2.ui.chat;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.mobile_programming_2025_2.data.ChatMessage;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository;
    private final LiveData<List<ChatMessage>> messages;

    public ChatViewModel(ChatRepository repository) {
        this.repository = repository;
        this.messages = repository.getMessages();
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public void sendMessage(String content) {
        if (content != null && !content.trim().isEmpty()) {
            repository.sendMessage(content.trim());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanupListener();
         Log.d("ChatViewModel", "Listener cleaned up.");
    }
}