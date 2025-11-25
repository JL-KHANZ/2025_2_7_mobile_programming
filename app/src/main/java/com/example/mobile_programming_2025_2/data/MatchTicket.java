package com.example.mobile_programming_2025_2.data;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * matchTickets/{ticketId}
 * 유저 한 명당 대기 티켓 1개
 * 매칭에 필요한 정보 + 매치 목록에 표시 정보 + 채팅방 생성시 필요한 정보 통합
 * matchTickets
 *  └─ {ticketId}
 *       uid: string          // 사용자 uid
 *       displayName: string|null
 *       emotionScores: {감정8개ㅔ 점수}
 *       topTwoEmotions: [감정 2개]
 *       status: "OPEN" or "CLOSED"
 *       createdAt: Timestamp
 *       updatedAt: Timestamp
 */
public class MatchTicket {
    public String uid;
    public String displayName;
    public Map<String, Integer> emotionScores;
    public List<String> topTwoEmotions;
    public String status;    //true: 이미 매칭에 사용된 티켓 , false: 아직 매칭 대기중
    @ServerTimestamp
    public Date createdAt;// 최초 생성 시 자동
    @ServerTimestamp
    public Date updatedAt; // 매 저장 시 자동

    public MatchTicket() {}
    public MatchTicket(String uid,
                       String displayName,
                       Map<String, Integer> emotionScores,
                       List<String> topTwoEmotions,
                       String status,
                       Date createdAt,
                       Date updatedAt) {
        this.uid = uid;
        this.displayName = displayName;
        this.emotionScores = emotionScores;
        this.topTwoEmotions = topTwoEmotions;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}