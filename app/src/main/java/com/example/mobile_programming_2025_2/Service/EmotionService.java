package com.example.mobile_programming_2025_2.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * users/{uid}/emotion 전용 서비스
 * (최근 3개월 유지 + 금일 감정 저장 + 기간별 감정 조회)
 *
 * 문서 구조 예시:
 * users/{uid}/emotion/{yyyy-MM-dd}
 * {
 *   date: "2025-11-09",
 *   emotions: { "분노":3, "공포":1, "기쁨":8, ... },
 *   top1: { "기쁨":8 },
 *   top2: { "슬픔":5 },
 *   createdAt: <Timestamp>,
 *   updatedAt: <Timestamp>
 * }
 */
public class EmotionService {

    private static final int BATCH_DELETE_LIMIT = 450; // Firestore 배치 제한 대비
    private static final int RETENTION_MONTHS   = 3;   // 최근 3개월만 유지

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /** 현재 로그인 사용자 UID 반환 */
    private @Nullable String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    /** users/{uid}/emotion 컬렉션 참조 */
    private CollectionReference emotionCol(@NonNull String uid) {
        return db.collection("users").document(uid).collection("emotion");
    }

    /** yyyy-MM-dd 형식 검증 */
    private boolean isValidYMD(String ymd) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setLenient(false);
            f.parse(ymd);
            return true;
        } catch (Exception e) { return false; }
    }

    /** 오늘 날짜 문자열 */
    private String todayYMD() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }

    /** 감정 Map에서 상위 K개 추출 (값 내림차순, 동점 시 키 사전순) */
    //util로 뺄 수 도 있음
    private List<Map.Entry<String,Integer>> topK(@NonNull Map<String,Integer> src, int k) {
        return src.entrySet().stream()
                .sorted(Comparator
                        .comparing(Map.Entry<String,Integer>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(Math.max(k, 0))
                .collect(Collectors.toList());
    }

    /**
     *  오늘 감정 저장 (문서ID = yyyy-MM-dd)
     *  감정 점수 Map<String, Integer> 저장
     *  top1/top2 비정규화 필드 함께 저장
     */
    public Task<Void> upsertToday(@NonNull Map<String, Integer> emotionScores) {
        String uid = currentUid();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("Not signed in"));

        String date = todayYMD();

        // 상위 2개 감정 추출
        List<Map.Entry<String,Integer>> top2 = topK(emotionScores, 2);
        Map<String, Integer> top1 = new HashMap<>();
        Map<String, Integer> top2Map = new HashMap<>();
        if (top2.size() >= 1) top1.put(top2.get(0).getKey(), top2.get(0).getValue());
        if (top2.size() >= 2) top2Map.put(top2.get(1).getKey(), top2.get(1).getValue());

        // Firestore 저장 데이터 구성
        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("emotions", emotionScores);
        if (!top1.isEmpty()) data.put("top1", top1);   // { "기쁨": 8 }
        if (!top2Map.isEmpty()) data.put("top2", top2Map); // { "슬픔": 5 }
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        // 저장 후 오래된 데이터 자동 정리 실행
        return emotionCol(uid)
                .document(date)
                .set(data, SetOptions.merge())
                .onSuccessTask(v -> pruneOldEmotionsAsync(uid));
    }

    /**
     *  특정 기간 조회 (yyyy-MM-dd ~ yyyy-MM-dd "포함")
     *  오늘~오늘 -> 당일만
     *  2025-11-01~2025-11-07 -> 그 기간 전체 반환
     *  단, 한달, 최근, 주간을 판별하는 날짜는 app수준에서 계산후 전달 필요!!
     *  날짜별 { date, emotions(Map<String,Integer>), top1, top2 }
     */
    public void getByPeriod(@NonNull String startDate,
                            @NonNull String endDate,
                            @NonNull OnSuccessListener<List<Map<String, Object>>> onSuccess,
                            @Nullable OnFailureListener onFailure) {

        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }

        // 날짜 형식 확인
        if (!isValidYMD(startDate) || !isValidYMD(endDate)) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalArgumentException("날짜 형식이 틀렸습니다.(예: yyyy-MM-dd)"));
            return;
        }

        // start <= end 보정
        if (startDate.compareTo(endDate) > 0) {
            String tmp = startDate; startDate = endDate; endDate = tmp;
        }

        emotionCol(uid)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("date", d.getString("date"));
                        item.put("emotions", d.get("emotions"));
                        item.put("top1", d.get("top1"));
                        item.put("top2", d.get("top2"));
                        list.add(item);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure != null ? onFailure : e -> {});
    }


    /** 3개월 이전(createdAt 기준) 문서들을 비동기로 반복 배치 삭제 */
    //추후 util로 뺄 수 도 있음

    private Task<Void> pruneOldEmotionsAsync(@NonNull String uid) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
        cal.add(Calendar.MONTH, -RETENTION_MONTHS);
        Date cutoff = cal.getTime();

        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        pruneBatch(uid, cutoff, null, new AtomicInteger(0), tcs);
        return tcs.getTask();
    }

    /** 페이지 단위(<=450개)로 createdAt < cutoff 문서를 삭제하고 다음 페이지로 재귀 진행 */
    private void pruneBatch(String uid, Date cutoff, @Nullable DocumentSnapshot lastDoc,
                            AtomicInteger deletedCount, TaskCompletionSource<Void> tcs) {
        Query q = emotionCol(uid)
                .whereLessThan("createdAt", cutoff)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(BATCH_DELETE_LIMIT);

        if (lastDoc != null)
            q = q.startAfter(lastDoc);

        q.get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                tcs.setResult(null);
                return;
            }

            WriteBatch batch = db.batch();
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs)
                batch.delete(d.getReference());

            batch.commit()
                    .addOnSuccessListener(v -> {
                        deletedCount.addAndGet(docs.size());
                        pruneBatch(uid, cutoff, docs.get(docs.size() - 1), deletedCount, tcs);
                    })
                    .addOnFailureListener(tcs::setException);
        }).addOnFailureListener(tcs::setException);
    }

    /** (선택) 수동 정리 트리거 — 앱 시작 시 호출 가능 */
    public Task<Void> pruneOldEmotionsNow() {
        String uid = currentUid();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("Not signed in"));
        return pruneOldEmotionsAsync(uid);
    }
}
