package com.example.mobile_programming_2025_2.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatMessageService {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase rtdb = FirebaseDatabase.getInstance();

    @Nullable
    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    /** /chatMessages/{roomId} 레퍼런스 */
    private DatabaseReference messagesRef(@NonNull String roomId) {
        return rtdb.getReference("chatMessages").child(roomId);
    }

    /**
     * roomId 방에 텍스트 메시지 전송
     */
    public void sendMessage(@NonNull String roomId,
                            @NonNull String text,
                            @Nullable DatabaseReference.CompletionListener listener) {

        String uid = currentUid();
        if (uid == null) {
            if (listener != null) {
                listener.onComplete(
                        DatabaseError.fromException(
                                new IllegalStateException("Not signed in")
                        ),
                        null
                );
            }
            return;
        }

        long now = System.currentTimeMillis();
        ChatMessage msg = new ChatMessage(uid, text, now);

        DatabaseReference ref = messagesRef(roomId).push(); // 새 messageId 생성
        ref.setValue(msg, listener);
    }

    /**
     * 방의 메시지를 실시간으로 가져옴
     */
    public ChildEventListener listenMessages(@NonNull String roomId,
                                             @NonNull ChildEventListener listener) {

        messagesRef(roomId)
                .orderByChild("createdAt")
                .addChildEventListener(listener);
        return listener;
    }

    /** 리스너 해제 */
    public void removeListener(@NonNull String roomId,
                               @NonNull ChildEventListener listener) {
        messagesRef(roomId).removeEventListener(listener);
    }

}

