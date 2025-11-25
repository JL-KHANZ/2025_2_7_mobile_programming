package com.example.mobile_programming_2025_2.data;

/**
 * 현재 사용자의 활성 채팅방 조회 결과 DTO
 * hasRoom == false 면 roomId/other... 는 무시해도 됨
 */
public class ActiveChatRoomDTO {
    public String roomId;          // chatRooms/{roomId}
    public String otherDisplayName;// 상대 표시 이름

    public ActiveChatRoomDTO() {
    }

    public ActiveChatRoomDTO(String roomId,
                             String otherDisplayName) {
        this.roomId = roomId;;
        this.otherDisplayName = otherDisplayName;
    }
}
