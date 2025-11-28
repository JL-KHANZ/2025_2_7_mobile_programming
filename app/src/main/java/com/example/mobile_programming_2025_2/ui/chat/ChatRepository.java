package com.example.mobile_programming_2025_2.ui.chat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile_programming_2025_2.Service.ChatMessageService;
import com.example.mobile_programming_2025_2.Service.ChatRoomService;
import com.example.mobile_programming_2025_2.Service.MatchService;
import com.example.mobile_programming_2025_2.data.CandidateDTO;
import com.example.mobile_programming_2025_2.data.ChatMessage;
import com.example.mobile_programming_2025_2.data.CreateRoomResultDTO;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    private final ChatRoomService roomService;
    private final ChatMessageService messageService;
    private final MatchService matchService;
    private final MutableLiveData<String> currentRoomId = new MutableLiveData<>();
    private final MutableLiveData<String> otherDisplayName = new MutableLiveData<>();
    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
    private ChatMessageService.Subscription chatSubscription;
    private final MutableLiveData<List<CandidateDTO>> candidateList = new MutableLiveData<>();
    private final MutableLiveData<CreateRoomResultDTO> createdRoomUser = new MutableLiveData<>();

    public ChatRepository(ChatRoomService roomService, ChatMessageService messageService, MatchService matchService) {
        this.roomService = roomService;
        this.messageService = messageService;
        this.matchService = matchService;
    }

    private void attachMessagesListener(String roomId) {
        if(roomId == null || roomId.isEmpty() || chatSubscription != null) return;
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
            chatSubscription = messageService.startListening(currentRoomId.toString(), listener);
        }
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }

    public void sendMessage(String content) {
        String roomId = currentRoomId.getValue();
        if (roomId != null) {
            messageService.sendMessage(roomId, content,
                    (err, ref) -> {
                        assert err != null;
                        System.out.println("Error sending message: " + content);
                    });
        } else {
            System.out.println("No active chat room");
        }
    }

    public MutableLiveData<String> getActiveChatRoom() {
        roomService.getActiveChatRoom(
                dto -> {
                    String roomId = dto.roomId;
                    String otherName = dto.otherDisplayName;
                    currentRoomId.setValue(roomId);
                    otherDisplayName.setValue(otherName);
                    System.out.println("ActiveChatRoom: " + roomId + ", " + otherDisplayName);

                    attachMessagesListener(roomId);
                },
                err -> {
                    currentRoomId.setValue(null);
                    System.out.println("Error getting active chat room: " + err.toString());
                });
        return currentRoomId;
    }

    public void cleanupListener() {
        if (chatSubscription != null) {
            messageService.stopListening(chatSubscription);
            chatSubscription = null;
        }
    }

    public MutableLiveData<List<CandidateDTO>> requestMatch() {
        matchService.requestMatchFromTodayDaily(
                dto -> {
                    List<CandidateDTO> candidates = dto.candidates;
                    candidateList.setValue(candidates);
                }
                , err -> {
                    candidateList.setValue(null);
                    System.out.println("Error requesting match: " + err);
                });
        return candidateList;
    }

    public MutableLiveData<CreateRoomResultDTO> createChatRoom(@NonNull String otherUserUid) {
        roomService.createChatRoomWith(
                otherUserUid,
                dto -> {
                    createdRoomUser.setValue(dto);
                },
                err -> {
                    createdRoomUser.setValue(null);
                    System.out.println("Error creating chat room: " + err);
                });
        return createdRoomUser;
    }

}