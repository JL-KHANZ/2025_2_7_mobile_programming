package com.example.mobile_programming_2025_2.data;

import java.util.List;

/**
 * 매칭 요청 결과:
 *  - 내가 가진 ticketId
 *  - 후보자 리스트
 */
public class MatchResultDTO {
    public List<CandidateDTO> candidates;

    public MatchResultDTO() {}

    public MatchResultDTO(List<CandidateDTO> candidates) {
        this.candidates = candidates;
    }
}
