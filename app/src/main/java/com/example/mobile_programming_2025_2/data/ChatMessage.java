package com.example.mobile_programming_2025_2.data;

public class ChatMessage {
    public String senderUid;  // 보낸 사람 uid
    public String text;       // 메시지 내용
    public long createdAt;    // System.currentTimeMillis() 기준 millis

    public ChatMessage() {
        // Firebase 역직렬화용 기본 생성자
    }

    public ChatMessage(String senderUid, String text, long createdAt) {
        this.senderUid = senderUid;
        this.text = text;
        this.createdAt = createdAt;
    }
}
