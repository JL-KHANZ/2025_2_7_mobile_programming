package com.example.mobile_programming_2025_2.ui.chat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile_programming_2025_2.Service.ChatMessageService;
import com.example.mobile_programming_2025_2.Service.ChatRoomService;
import com.example.mobile_programming_2025_2.data.ChatMessage;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private final ChatRoomService roomService;
    private final ChatMessageService messageService;
    private final String currentRoomId = "default_room_id";
    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
    private ChildEventListener messagesListener;

    public ChatRepository(ChatRoomService roomService, ChatMessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
        attachMessagesListener();
    }

    private void attachMessagesListener() {
        messagesLiveData.setValue(new ArrayList<>());

        if (messagesListener == null) {
            messagesListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    ChatMessage newMessage = snapshot.getValue(ChatMessage.class);
                    if (newMessage != null) {
                        List<ChatMessage> currentList = messagesLiveData.getValue();
                        if (currentList != null) {
                            currentList.add(newMessage);
                            messagesLiveData.postValue(currentList);
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // Handle edited messages if needed
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    // Handle deleted messages if needed
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // Handle moved messages if needed
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatRepository", "Database listener cancelled", error.toException());
                }
            };
            messageService.listenMessages(currentRoomId, messagesListener);
        }
    }
    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }
    public void sendMessage(String content) {
        String currentUserId = "user_A"; // Replace with actual Auth UID logic
        long timestamp = System.currentTimeMillis();
        ChatMessage message = new ChatMessage(currentUserId, content, timestamp);

        messageService.sendMessage(currentRoomId, content,
                (err, ref) -> {
                    Log.e("ChatRepository", "Error sending message" + err.toString());
                });
    }

    public void cleanupListener() {
        if (messagesListener != null) {
            // messageService.removeListener(currentRoomId, messagesListener);
            // 위 함수 필요!
            messagesListener = null;
        }
    }
}
