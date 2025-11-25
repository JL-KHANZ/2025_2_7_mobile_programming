package com.example.mobile_programming_2025_2.ui.chat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile_programming_2025_2.Service.ChatMessageService;
import com.example.mobile_programming_2025_2.Service.ChatRoomService;
import com.example.mobile_programming_2025_2.data.ChatMessage;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private final ChatRoomService roomService;
    private final ChatMessageService messageService;
    private final String currentRoomId = "default_room_id";
    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
    private ChatMessageService.Subscription chatSubscription;

    public ChatRepository(ChatRoomService roomService, ChatMessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
        attachMessagesListener();
    }

    private void attachMessagesListener() {
        messagesLiveData.setValue(new ArrayList<>());

        if (chatSubscription == null) {

            ChatMessageService.MessageListener listener = new ChatMessageService.MessageListener() {

                @Override
                public void onMessageAdded(String key, ChatMessage newMessage) {
                    List<ChatMessage> currentList = messagesLiveData.getValue();
                    if (currentList != null) {
                        currentList.add(newMessage);
                        messagesLiveData.postValue(currentList);
                    }
                }

                @Override
                public void onError(DatabaseError error) {
                    Log.e("ChatRepository", "Database listener error: " + error.toException());
                }
            };
            chatSubscription = messageService.startListening(currentRoomId, listener);
        }
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }

    public void sendMessage(String content) {
        messageService.sendMessage(currentRoomId, content,
                (err, ref) -> {
                    Log.e("ChatRepository", "Error sending message: " + err.toString());
                });
    }

    public void cleanupListener() {
        if (chatSubscription != null) {
            messageService.stopListening(chatSubscription);
            chatSubscription = null;
        }
    }
}