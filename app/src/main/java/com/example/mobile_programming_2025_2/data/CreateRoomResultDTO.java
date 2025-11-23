package com.example.mobile_programming_2025_2.data;

/**
 * Cloud Function createRoomFromMatch의 결과를 담는 DTO
 * {
 *   roomId: string,
 *   otherUid: string,
 * }
 */
public class CreateRoomResultDTO {

    public String roomId;
    public String otherDisplayName;


    public CreateRoomResultDTO() {
    }

    public CreateRoomResultDTO(String roomId, String otherDisplayName) {
        this.roomId = roomId;
        this.otherDisplayName = otherDisplayName;
    }
}
