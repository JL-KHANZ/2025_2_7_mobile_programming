package com.example.mobile_programming_2025_2.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.UserDiary;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

//firestore 문서 크기 제한(공식 기준)
//한 문서(document)의 최대 크기는 1 MiB (1,048,576 bytes)
//이 크기 안에 필드 이름 + 값 전체 + 메타데이터가 모두 포함됨
//대략 1k~10k자 정도까진 여유

/** users/{uid}/diary 전용 서비스 (최근 3개월 유지 + 금일 저장 + 임의기간 조회) */
public class DiaryService {

    private static final int BATCH_DELETE_LIMIT = 450; // Firestore 배치 500 제한 대비 여유
    private static final int RETENTION_MONTHS   = 3;   // 보존 개월 수 (최근 3개월만 유지)

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /** 현재 로그인 사용자 UID */
    private @Nullable String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    /** users/{uid}/diary 컬렉션 참조 */
    private CollectionReference diaryCol(@NonNull String uid) {
        return db.collection("users").document(uid).collection("diary");
    }

    /** yyyy-MM-dd 형식 검증  */
    private boolean isValidYMD(String ymd) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US); //로컬 영향을 받지 않기 위해 us
            f.setLenient(false);
            f.parse(ymd);
            return true;
        } catch (Exception e) { return false; }
    }

    /** 다음 달 접두사(yyyy-MM) */
    private String nextMonthKey(int year, int month1to12) {
        int y = year, m = month1to12 + 1;
        if (m == 13) { y += 1; m = 1; }
        return String.format(Locale.US, "%04d-%02d", y, m);
    }

    /** 오늘 일기 업서트(문서ID=yyyy-MM-dd) + 저장 성공 후 3개월 이전 문서 자동 정리 */
    public Task<Void> upsertToday(@NonNull String title, @NonNull String content) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date());

        UserDiary entry = new UserDiary(title, content, date, time);

        return diaryCol(uid).document(date)
                .set(entry, SetOptions.merge())
                .onSuccessTask(v -> pruneOldDiariesAsync(uid));
    }

    /**
     * 특정 기간 조회 (yyyy-MM-dd ~ yyyy-MM-dd "포함")
     * 오늘~오늘을 넣으면 당일, 2025-11-01~2025-11-07 넣으면 그 기간 전체 반환
     * 월이 바뀌는 기간: 2025-01-27 ~ 2025-02-04 같은 형식도 커버 가능
     * 단, 한달, 최근, 주간을 판별하는 날짜는 app수준에서 계산후 전달 필요!!
     */
    public void getByPeriod(@NonNull String startDate,
                            @NonNull String endDate,
                            @NonNull OnSuccessListener<List<UserDiary>> onSuccess,
                            @Nullable OnFailureListener onFailure) {
        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null) onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }
        if (!isValidYMD(startDate) || !isValidYMD(endDate)) {
            if (onFailure != null) onFailure.onFailure(new IllegalArgumentException("날짜 형식이 틀렸습니다.( ex : yyyy-MM-dd)"));
            return;
        }
        // start <= end로 보정  (혹시나 모를 실수 방지)
        if (startDate.compareTo(endDate) > 0) {
            String tmp = startDate; startDate = endDate; endDate = tmp;
        }

        diaryCol(uid)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<UserDiary> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        UserDiary ud = d.toObject(UserDiary.class);
                        if (ud != null) list.add(ud);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure != null ? onFailure : e -> {});
    }

    /** 3개월 이전(createdAt 기준) 문서들을 비동기로 반복 배치 삭제 */
    private Task<Void> pruneOldDiariesAsync(@NonNull String uid) {
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
        Query q = diaryCol(uid)
                .whereLessThan("createdAt", cutoff)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(BATCH_DELETE_LIMIT);

        if (lastDoc != null) q = q.startAfter(lastDoc);

        q.get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                tcs.setResult(null);
                return;
            }
            WriteBatch batch = db.batch();
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs) batch.delete(d.getReference());

            batch.commit()
                    .addOnSuccessListener(v -> {
                        deletedCount.addAndGet(docs.size());
                        pruneBatch(uid, cutoff, docs.get(docs.size()-1), deletedCount, tcs);
                    })
                    .addOnFailureListener(tcs::setException);
        }).addOnFailureListener(tcs::setException);
    }

}
