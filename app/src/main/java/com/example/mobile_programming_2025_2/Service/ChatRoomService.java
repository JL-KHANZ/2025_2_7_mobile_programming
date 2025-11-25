package com.example.mobile_programming_2025_2.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.ActiveChatRoomDTO;
import com.example.mobile_programming_2025_2.data.CreateRoomResultDTO;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class ChatRoomService {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    /** 현재 로그인 uid */
    @Nullable
    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }
    //================================================================================
    /**
     *선택한 상대와 방 생성/재사용
     */
    public void createChatRoomWith(@NonNull String otherUid,
                                   @NonNull OnSuccessListener<CreateRoomResultDTO> onSuccess,
                                   @Nullable OnFailureListener onFailure) {
        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }
        if (otherUid.isEmpty() || otherUid.equals(uid)) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalArgumentException("잘못된 otherUid 입니다."));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("otherUid", otherUid);

        functions
                .getHttpsCallable("createChatRoom")
                .call(payload)
                .addOnSuccessListener(res -> {
                    try {
                        CreateRoomResultDTO dto = parseCreateRoomResult(res);
                        onSuccess.onSuccess(dto);
                    } catch (Exception e) {
                        if (onFailure != null) onFailure.onFailure(e);
                    }
                })
                .addOnFailureListener(err -> {
                    if (onFailure != null) onFailure.onFailure(err);
                });
    }

    private CreateRoomResultDTO parseCreateRoomResult(@NonNull HttpsCallableResult result) {
        Object dataObj = result.getData();
        if (!(dataObj instanceof Map)) {
            throw new IllegalStateException("Unexpected response from createChatRoom");
        }
        Map<?, ?> data = (Map<?, ?>) dataObj;

        String roomId = data.get("roomId") != null
                ? String.valueOf(data.get("roomId"))
                : null;

        String otherDisplayName = data.get("otherDisplayName") != null
                ? String.valueOf(data.get("otherDisplayName"))
                : "익명의 사용자";

        return new CreateRoomResultDTO(roomId, otherDisplayName);
    }
    //================================================================================
    /**
     *현재 사용자가 참여중인 방(있으면 1개)을 조회
     *hasRoom == false: 방 없음
     *hasRoom == true: roomId, otherDisplayName 채워져 있음
     */
    public void getActiveChatRoom(@NonNull OnSuccessListener<ActiveChatRoomDTO> onSuccess,
                                  @Nullable OnFailureListener onFailure) {
        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }

        functions
                .getHttpsCallable("getActiveChatRoom")
                .call()
                .addOnSuccessListener(res -> {
                    try {
                        ActiveChatRoomDTO dto = parseActiveRoomResult(res);
                        onSuccess.onSuccess(dto);
                    } catch (Exception e) {
                        if (onFailure != null) onFailure.onFailure(e);
                    }
                })
                .addOnFailureListener(err -> {
                    if (onFailure != null) onFailure.onFailure(err);
                });
    }

    private ActiveChatRoomDTO parseActiveRoomResult(@NonNull HttpsCallableResult result) {
        Object dataObj = result.getData();
        if (!(dataObj instanceof Map)) {
            throw new IllegalStateException("Unexpected response from getActiveChatRoom");
        }
        Map<?, ?> data = (Map<?, ?>) dataObj;

        Object hasRoomObj = data.get("hasRoom");
        boolean hasRoom = (hasRoomObj instanceof Boolean) && (Boolean) hasRoomObj;

        if (!hasRoom) {
            return new ActiveChatRoomDTO(null, null);
        }

        String roomId = data.get("roomId") != null
                ? String.valueOf(data.get("roomId"))
                : null;

        String otherDisplayName = data.get("otherDisplayName") != null
                ? String.valueOf(data.get("otherDisplayName"))
                : "익명의 사용자";

        return new ActiveChatRoomDTO(roomId, otherDisplayName);
    }
    //=============================================================================
    /**
     * 채팅 종료: 방 삭제 + 두 유저 activeRoomId = null + RTDB 메시지 삭제
     */
    public void closeChatRoom(@NonNull String roomId,
                              @NonNull OnSuccessListener<Void> onSuccess,
                              @Nullable OnFailureListener onFailure) {

        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("Not signed in"));
            }
            return;
        }
        if (roomId.isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalArgumentException("roomId가 비어 있습니다."));
            }
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);

        functions
                .getHttpsCallable("closeChatRoom")
                .call(payload)
                .addOnSuccessListener(res -> {
                    // CF는 { success: true } 정도만 반환하므로 여기서는 그냥 성공만 전달
                    onSuccess.onSuccess(null);
                })
                .addOnFailureListener(err -> {
                    if (onFailure != null) onFailure.onFailure(err);
                });
    }
}

