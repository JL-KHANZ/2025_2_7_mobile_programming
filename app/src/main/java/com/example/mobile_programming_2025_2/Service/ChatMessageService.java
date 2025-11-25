package com.example.mobile_programming_2025_2.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
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
//====================================================================================

    /** 메시지 수신
     * 프론트가 구현하면 되는 콜백 인터페이스 */
    public interface MessageListener {
        /** 새 메시지 추가됨 */
        void onMessageAdded(@NonNull String messageId, @NonNull ChatMessage msg);

        /** 기존 메시지 변경됨 (거의 안 쓸 수도 있음) */
        default void onMessageChanged(@NonNull String messageId, @NonNull ChatMessage msg) {}

        /** 메시지 삭제됨 (필요 없으면 구현 안 해도 됨) */
        default void onMessageRemoved(@NonNull String messageId) {}

        /** 에러 발생 */
        default void onError(@NonNull DatabaseError error) {}
    }

    /** 구독 해제할 때 사용할 핸들 객체 */
    public static class Subscription {
        public final String roomId;
        final ChildEventListener internalListener;

        Subscription(String roomId, ChildEventListener l) {
            this.roomId = roomId;
            this.internalListener = l;
        }
    }

    /**
     * 채팅방 메시지 실시간 수신 시작
     */
    public Subscription startListening(@NonNull String roomId,
                                       @NonNull MessageListener listener) {

        ChildEventListener cel = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snap, @Nullable String previousChildName) {
                ChatMessage msg = snap.getValue(ChatMessage.class);
                if (msg != null) {
                    listener.onMessageAdded(snap.getKey(), msg);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snap, @Nullable String previousChildName) {
                // 사용 안함 아직 기능 없음
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snap) {
                // 사용 안함 아직 기능 없음
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snap, @Nullable String previousChildName) {
                // 사용 안 함 아직 기능 없음
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        };

        messagesRef(roomId)
                .orderByChild("createdAt")
                .addChildEventListener(cel);

        return new Subscription(roomId, cel);
    }

    /**
     * 실시간 수신 해제
     */
    public void stopListening(@Nullable Subscription subscription) {
        if (subscription == null) return;
        messagesRef(subscription.roomId)
                .removeEventListener(subscription.internalListener);
    }
}

