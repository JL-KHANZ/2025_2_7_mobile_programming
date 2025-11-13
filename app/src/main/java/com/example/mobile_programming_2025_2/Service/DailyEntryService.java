package com.example.mobile_programming_2025_2.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.Daily;
import com.example.mobile_programming_2025_2.data.DailyEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
// "2025-11-01" : DailyEntry {
//    date = "2025-11-01",
//            content = "오랜만에 친구를 만나서 즐거웠다!",
//            feedback = { wormWord = "오늘도 충분히 잘해내고 있어요",
//                    mindset = "긍정적인 마음가짐이 중요합니다",
//                    feedback1 = "친구와의 관계는 큰 힘이 돼요",
//                    feedback2 = "적당한 휴식도 필요해요"},
//            emotions = { "기쁨" = 9, "신뢰" = 6, "슬픔" = 1, … },
//    topEmotion = "기쁨",
//            createdAt = 2025-11-01T10:23:43Z,
//            updatedAt = 2025-11-01T10:30:12Z


/** users/{uid}/dailyEntry 전용 서비스 (최근 3개월 유지 + 금일 저장 + 임의기간 조회) */
public class DailyEntryService {
    public static final String TAG = "DailyEntryService";

    private static final int BATCH_DELETE_LIMIT = 450; // Firestore 배치 500 제한 대비 여유
    private static final int RETENTION_MONTHS   = 3;   // 보존 개월 수 (최근 3개월만 유지)

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private String cachedDate = null; // 최초 1번만 세팅

    /** 현재 로그인 사용자 UID */
    private @Nullable String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    /** 오늘 날짜 문자열 */
    //todo: app에서 날짜 세팅해줘야함!!!!!!
    public void setDate(@NonNull String date) {
        if (!isValidYMD(date)) {
            throw new IllegalArgumentException("Invalid date format (expected: yyyy-MM-dd)");
        }
        this.cachedDate = date;
    }

    /** users/{uid}/dailyEntry 컬렉션 참조 */
    private DocumentReference todayDoc(@NonNull String uid) {
        String date = cachedDate;
        // users/{uid}/dailyEntry/{yyyy-MM-dd}
        return db.collection("users")
                .document(uid)
                .collection("dailyEntry")
                .document(date);
    }

    /** yyyy-MM-dd 형식 검증*/
    private boolean isValidYMD(String ymd) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US); //로컬 영향을 받지 않기 위해 us
            f.setLenient(false);
            f.parse(ymd);
            return true;
        } catch (Exception e) { return false; }
    }
//===========================================================================service
    /** 오늘 일기 업서트(문서ID=yyyy-MM-dd)*/
    public Task<Void> upsertTodayContent( @NonNull String content) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Not signed in"));
        if (cachedDate == null) return Tasks.forException(new IllegalStateException("Date not set. Call setDate(date) first."));


        String date = cachedDate;
        DocumentReference docRef = todayDoc(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);                                 // 문서에 날짜 보장
        data.put("content", content);                           // 오늘 일기 본문
        data.put("updatedAt", FieldValue.serverTimestamp());    //업데이트 기입
        return docRef.get().continueWithTask(task -> {
                    DocumentSnapshot snap = task.getResult();

                    // createdAt 최초 생성 시 1회 추가
                    if (!snap.exists()) {
                        data.put("createdAt", FieldValue.serverTimestamp());
                    }

                    return docRef.set(data, SetOptions.merge());
                }).continueWithTask(t -> pruneOldDailyEntriesAsync(uid))
                .addOnSuccessListener(v -> Log.d(TAG, "오늘 일기 업서트 + 3개월 정리 완료: " + date))
                .addOnFailureListener(e -> Log.w(TAG, "오늘 일기 업서트 실패", e));
    }

    /** 오늘 감정 업서트(문서ID=yyyy-MM-dd)*/
    public Task<Void> upsertTodayEmotion(@NonNull Map<String, Integer>emotions,
                                         @NonNull String topEmotion) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Not signed in"));
        if (cachedDate == null) return Tasks.forException(new IllegalStateException("Date not set. Call setDate(date) first."));


        String date = cachedDate;
        DocumentReference docRef = todayDoc(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);                                 // 문서에 날짜 보장
        data.put("topEmotion", topEmotion);                     // 메인 감정
        data.put("emotions", emotions);                         // 오늘 일기 본문
        data.put("updatedAt", FieldValue.serverTimestamp());    //업데이트 기입

        return docRef
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "오늘 감정 업서트 완료: " + date))
                .addOnFailureListener(e -> Log.w(TAG, "오늘 감정 업서트 실패", e));
    }

    /** 오늘 피드백 업서트(문서ID=yyyy-MM-dd)*/
    public Task<Void> upsertTodayFeedback(@NonNull Map<String,String> feedback) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Not signed in"));
        if (cachedDate == null) return Tasks.forException(new IllegalStateException("Date not set. Call setDate(date) first."));


        String date = cachedDate;
        DocumentReference docRef = todayDoc(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);                                 // 문서에 날짜 보장
        data.put("feedback", feedback);                         // 피드백
        data.put("updatedAt", FieldValue.serverTimestamp());    // 업데이트 기입

        return docRef
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "오늘 피드백 업서트 완료: " + date))
                .addOnFailureListener(e -> Log.w(TAG, "오늘 피드백 업서트 실패", e));
    }

    /**
     * 특정 기간 조회 (yyyy-MM-dd ~ yyyy-MM-dd "포함")
     * 오늘~오늘을 넣으면 당일, 2025-11-01~2025-11-07 넣으면 그 기간 전체 반환
     * 월이 바뀌는 기간: 2025-01-27 ~ 2025-02-04 같은 형식도 커버 가능
     * 단, 한달, 최근, 주간을 판별하는 날짜는 app수준에서 계산후 전달 필요!!
     */
    public void getByPeriod(
            @NonNull String startDate,
            @NonNull String endDate,
            @NonNull OnSuccessListener<Map<String, DailyEntry>> onSuccess,
            @Nullable OnFailureListener onFailure
    ) {
        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }

        // 날짜 형식 체크
        if (!isValidYMD(startDate) || !isValidYMD(endDate)) {
            if (onFailure != null)
                onFailure.onFailure(new IllegalArgumentException("날짜 형식 오류 (yyyy-MM-dd)"));
            return;
        }

        // start <= end 보정
        if (startDate.compareTo(endDate) > 0) {
            String tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        db.collection("users")
                .document(uid)
                .collection("dailyEntry")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {

                    // 날짜 순서 보존을 위해 LinkedHashMap 사용
                    Map<String, DailyEntry> result = new LinkedHashMap<>();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        DailyEntry entry = d.toObject(DailyEntry.class);
                        if (entry == null) continue;

                        result.put(entry.date, entry);  // 날짜를 key로
                    }

                    onSuccess.onSuccess(result);
                })
                .addOnFailureListener(onFailure != null ? onFailure : e -> {});
    }



    //=======================================================================3개월 단위 관리 util
    /** 3개월 이전(createdAt 기준) 문서들을 비동기로 반복 배치 삭제 */
    private Task<Void> pruneOldDailyEntriesAsync(@NonNull String uid) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
        cal.add(Calendar.MONTH, -RETENTION_MONTHS);
        Date cutoff = cal.getTime();  // 오늘 - 3개월

        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        pruneDailyBatch(uid, cutoff, null, new AtomicInteger(0), tcs);
        return tcs.getTask();
    }

    /** 페이지 단위(<=450개)로 createdAt < cutoff 문서를 삭제하고 다음 페이지로 재귀 진행 */
    private void pruneDailyBatch(
            String uid,
            Date cutoff,
            @Nullable DocumentSnapshot lastDoc,
            AtomicInteger deletedCount,
            TaskCompletionSource<Void> tcs
    ) {
        // users/{uid}/dailyEntry 에서 createdAt < cutoff 조건으로 조회
        Query q = db.collection("users")
                .document(uid)
                .collection("dailyEntry")
                .whereLessThan("createdAt", cutoff)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(BATCH_DELETE_LIMIT);

        if (lastDoc != null) q = q.startAfter(lastDoc);

        q.get().addOnSuccessListener(snap -> {
            // 더 이상 삭제할 문서 없음
            if (snap.isEmpty()) {
                Log.d(TAG, " 3개월 이전 DailyEntry 정리 완료. 총 삭제: " + deletedCount.get());
                tcs.setResult(null);
                return;
            }
            WriteBatch batch = db.batch();
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs) {
                batch.delete(d.getReference());
            }

            batch.commit()
                    .addOnSuccessListener(v -> {
                        deletedCount.addAndGet(docs.size());
                        Log.d(TAG, " 삭제 진행 중 & 현재까지 삭제: " + deletedCount.get());
                        pruneDailyBatch(uid, cutoff, docs.get(docs.size() - 1), deletedCount, tcs);
                    })
                    .addOnFailureListener(tcs::setException);

        }).addOnFailureListener(tcs::setException);
    }
}
