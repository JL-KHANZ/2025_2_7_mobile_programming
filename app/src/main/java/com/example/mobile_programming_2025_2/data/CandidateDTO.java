package com.example.mobile_programming_2025_2.data;

import java.util.List;

/**
 * 매칭 후보 한 명에 대한 정보 DTO
 *
 * uid            : 상대방 uid (실제 채팅 상대 식별용, 앱에서는 노출하지 않음)
 * displayName    : 화면에 보여줄 이름 (email @ 앞부분 등)
 * topTwoEmotions : 점수 상위 2개 감정 이름 리스트 (예: ["기쁨", "신뢰"])
 */
public class CandidateDTO {

    public String uid;
    public String displayName;
    public List<String> topTwoEmotions;

    public CandidateDTO() {
    }

    public CandidateDTO(String uid,
                        String displayName,
                        List<String> topTwoEmotions) {
        this.uid = uid;
        this.displayName = displayName;
        this.topTwoEmotions = topTwoEmotions;
    }
}
