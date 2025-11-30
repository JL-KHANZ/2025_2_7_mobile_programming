package com.example.mobile_programming_2025_2.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.CandidateDTO;
import com.example.mobile_programming_2025_2.data.MatchResultDTO;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import java.util.Locale;
import java.util.Map;

public class MatchService {

    private static final String TAG = "MatchService";
    private static final int MAX_CANDIDATES = 5;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    /** 현재 로그인 uid */
    @Nullable
    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    /** users/{uid}/dailyEntry 컬렉션 */
    private CollectionReference dailyEntryCol(@NonNull String uid) {
        return db.collection("users")
                .document(uid)
                .collection("dailyEntry");
    }

    /** 오늘 날짜 yyyy-MM-dd */
    private String todayYMD() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                .format(new Date());
    }

    /**
     *오늘 일기의 emotions(map) 을 읽어서
     *Cloud Function "requestMatch" 호출
     *MatchResultDTO(ticketId, candidates) 반환
     */
    public void requestMatchFromTodayDaily(@NonNull OnSuccessListener<MatchResultDTO> onSuccess,
                                           @Nullable OnFailureListener onFailure) {

        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }

        String uid = u.getUid();
        String today = todayYMD();

        Log.d(TAG, "requestMatchFromTodayDaily uid=" + uid + ", date=" + today);

        dailyEntryCol(uid).document(today)
                .get()
                .addOnSuccessListener(snap -> handleDailyEntryAndCallFunction(
                        snap, uid, onSuccess, onFailure
                ))
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    /** dailyEntry 문서에서 emotions를 꺼내고, requestMatch 함수 호출 */
    private void handleDailyEntryAndCallFunction(DocumentSnapshot snap,
                                                 String uid,
                                                 OnSuccessListener<MatchResultDTO> onSuccess,
                                                 @Nullable OnFailureListener onFailure) {
        if (snap == null || !snap.exists()) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("오늘 일기가 없습니다."));
            return;
        }

        Object rawEmotions = snap.get("emotions");
        if (!(rawEmotions instanceof Map)) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("오늘 감정 데이터가 없습니다."));
            return;
        }

        // Firestore에서 가져온 Map<String, Object> → Map<String, Integer>
        Map<?, ?> rawMap = (Map<?, ?>) rawEmotions;
        Map<String, Integer> emotionScores = new HashMap<>();
        for (Map.Entry<?, ?> e : rawMap.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            String key = String.valueOf(e.getKey());
            Object v = e.getValue();
            if (v instanceof Number) {
                emotionScores.put(key, ((Number) v).intValue());
            } else {
                try {
                    emotionScores.put(key, Integer.parseInt(String.valueOf(v)));
                } catch (NumberFormatException ignore) {
                }
            }
        }

        if (emotionScores.isEmpty()) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("감정 점수가 비어 있습니다."));
            return;
        }

        // Cloud Function payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", uid);
        payload.put("emotionScores", emotionScores);

        functions
                .getHttpsCallable("requestMatch")
                .call(payload)
                .addOnSuccessListener(result -> {
                    try {
                        MatchResultDTO dto = parseMatchResult(result);
                        onSuccess.onSuccess(dto);
                    } catch (Exception ex) {
                        if (onFailure != null) onFailure.onFailure(ex);
                    }
                })
                .addOnFailureListener(err -> {
                    if (onFailure != null) onFailure.onFailure(err);
                });
    }

    /** Cloud Function 응답 -> MatchResultDTO 파싱 */
    private MatchResultDTO parseMatchResult(@NonNull HttpsCallableResult result) {
        Object dataObj = result.getData();
        if (!(dataObj instanceof Map)) {
            throw new IllegalStateException("Unexpected response type from requestMatch");
        }
        Map<?, ?> data = (Map<?, ?>) dataObj;
        List<CandidateDTO> candidates = new ArrayList<>();

        Object candObj = data.get("candidates");
        if (candObj instanceof List<?>) {
            List<?> rawList = (List<?>) candObj;
            int count = 0;
            for (Object o : rawList) {
                if (!(o instanceof Map<?, ?>)) continue;
                Map<?, ?> m = (Map<?, ?>) o;

                String uid = m.get("uid") != null ? String.valueOf(m.get("uid")) : null;
                String displayName = m.get("displayName") != null
                        ? String.valueOf(m.get("displayName"))
                        : "익명의 사용자";

                List<String> topTwo = new ArrayList<>();
                Object topTwoObj = m.get("topTwoEmotions");
                if (topTwoObj instanceof List<?>) {
                    for (Object tv : (List<?>) topTwoObj) {
                        if (tv != null) topTwo.add(String.valueOf(tv));
                    }
                }

                candidates.add(new CandidateDTO(uid, displayName, topTwo));

                count++;
                if (count >= MAX_CANDIDATES) break;
            }
        }

        return new MatchResultDTO(candidates);
    }
}
