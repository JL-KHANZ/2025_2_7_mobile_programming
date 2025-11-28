package com.example.mobile_programming_2025_2.ui.chat.candidate;

import com.example.mobile_programming_2025_2.data.CandidateDTO;

/**
 * 후보자 아이템 클릭 이벤트를 Fragment로 전달하기 위한 콜백 인터페이스
 */
public interface CandidateItemCallback {
    void onCandidateSelected(CandidateDTO candidate);
}