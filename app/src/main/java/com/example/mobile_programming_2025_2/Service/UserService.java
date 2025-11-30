package com.example.mobile_programming_2025_2.Service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mobile_programming_2025_2.data.ProfileUpdate;
import com.example.mobile_programming_2025_2.data.ServiceResult;
import com.example.mobile_programming_2025_2.data.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource; // 추가됨
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

//   profile                         ← (collection)
//        └─ info                        ← (document)  ← 프로필 문서 저장 위치
//          ├─ uid: string
//          ├─ email: string
//          ├─ name: string
//          ├─ nickname: string
//          ├─ birthdate: "YYYY-MM-DD"
//          ├─ photoURL: string
//          ├─ createdAt: Timestamp
//          └─ updatedAt: Timestamp

public class UserService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final String DEFAULT_NAME = "";
    private static final String DEFAULT_NICKNAME = "";
    private static final String DEFAULT_BIRTHDATE = "";
    private static final String DEFAULT_PHOTO_URL = null;

    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    // ⭐️ 주의: SignUpService와 경로가 일치해야 데이터가 보입니다.
    // 만약 SignUpService가 "users/{uid}"에 저장한다면, 여기도 맞춰야 합니다.
    // 현재 코드는 "users/{uid}/profile/info"를 바라보고 있습니다.
    private String docPath(String uid) {
        // return "users/" + uid; // (구조가 단순하다면 이걸 사용)
        return "users/" + uid + "/profile/info"; // (기존 코드 유지)
    }

    /** 현재 로그인된 사용자(Auth) 기반으로 기본 프로필 업서트 (회원가입 직후 호출) */
    public Task<Void> upsertProfileFromCurrentUser() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        String uid = currentUid();
        DocumentReference docRef = db.document(docPath(uid));

        return docRef.get().continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    var snap = task.getResult();
                    Map<String, Object> toSet = new HashMap<>();

                    if (snap == null || !snap.exists() || !snap.contains("uid")) {
                        toSet.put("uid", uid);
                    }
                    if ((snap == null || !snap.contains("email")) && u.getEmail() != null) {
                        toSet.put("email", u.getEmail());
                    }

                    if (snap == null || !snap.contains("name")) {
                        toSet.put("name", DEFAULT_NAME);
                    }
                    if (snap == null || !snap.contains("nickname")) {
                        toSet.put("nickname", DEFAULT_NICKNAME);
                    }
                    if (snap == null || !snap.contains("birthdate")) {
                        toSet.put("birthdate", DEFAULT_BIRTHDATE);
                    }

                    if (snap == null || !snap.contains("createdAt")) {
                        toSet.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    }
                    toSet.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    if (toSet.isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    return docRef.set(toSet, SetOptions.merge());
                })
                .addOnSuccessListener(v -> Log.d("UserService", "기본 프로필 업서트 완료: " + uid))
                .addOnFailureListener(e -> Log.e("UserService", "기본 프로필 업서트 실패", e));
    }

    /** 프로필 변경 사항 저장 (통합) */
    public Task<Void> saveMyProfile(@NonNull ProfileUpdate update) {
        String uid = currentUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        Map<String, Object> data = new HashMap<>();

        if (update.name != null)      data.put("name", update.name);
        if (update.nickname != null)  data.put("nickname", update.nickname);
        if (update.birthdate != null) data.put("birthdate", update.birthdate);
        data.put("uid", uid);

        DocumentReference doc = db.document(docPath(uid));

        if (update.photoUri == null) {
            return doc.set(data, SetOptions.merge());
        }

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("profiles/" + uid + "/photo.jpg");

        return ref.putFile(update.photoUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    data.put("photoURL", task.getResult().toString());
                    return doc.set(data, SetOptions.merge());
                });
    }

    /** ⭐️ [추가됨] 닉네임 변경 메서드 */
    public Task<ServiceResult> changeNickname(String newNickname) {
        TaskCompletionSource<ServiceResult> tcs = new TaskCompletionSource<>();
        String uid = currentUid();

        if (uid == null) {
            tcs.setResult(new ServiceResult(false, "로그인이 필요합니다."));
            return tcs.getTask();
        }

        // Firestore 업데이트
        db.document(docPath(uid))
                .update("nickname", newNickname)
                .addOnSuccessListener(aVoid -> {
                    tcs.setResult(new ServiceResult(true, "닉네임이 변경되었습니다."));
                })
                .addOnFailureListener(e -> {
                    tcs.setResult(new ServiceResult(false, "변경 실패: " + e.getMessage()));
                });

        return tcs.getTask();
    }

    /** 내 프로필 가져오기 */
    public void getMyProfile(OnSuccessListener<UserProfile> onSuccess,
                             OnFailureListener onFailure) {
        String uid = currentUid();
        if (uid == null) {
            if (onFailure != null) onFailure.onFailure(new IllegalStateException("Not signed in"));
            return;
        }

        db.document(docPath(uid))
                .get()
                .addOnSuccessListener(snap -> {
                    UserProfile p = snap.toObject(UserProfile.class);
                    if (onSuccess != null) onSuccess.onSuccess(p);
                })
                .addOnFailureListener(onFailure != null ? onFailure : e -> {});
    }

    /** 비밀번호 변경 */
    public Task<ServiceResult> changePassword(@NonNull String currentPassword, @NonNull String newPassword) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(new ServiceResult(false, "로그인이 필요합니다."));
        }

        String email = user.getEmail();
        if (email == null) {
            return Tasks.forResult(new ServiceResult(false, "이메일 정보를 찾을 수 없습니다."));
        }

        if (newPassword.length() < 6) {
            return Tasks.forResult(new ServiceResult(false, "새 비밀번호는 6자 이상이어야 합니다."));
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        return user.reauthenticate(credential)
                .onSuccessTask(v -> user.updatePassword(newPassword))
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return new ServiceResult(true, "비밀번호가 성공적으로 변경되었습니다.");
                    }
                    Exception e = task.getException();
                    String msg = convertError(e);
                    return new ServiceResult(false, msg);
                });
    }

    /** 에러 메시지 변환 */
    private String convertError(Exception e) {
        if (e == null) return "알 수 없는 오류가 발생했습니다.";
        String raw = e.getMessage();
        if (raw == null) return "알 수 없는 오류가 발생했습니다.";

        raw = raw.toLowerCase();

        if (raw.contains("password is invalid") || raw.contains("invalid password")) {
            return "현재 비밀번호가 올바르지 않습니다.";
        }
        if (raw.contains("weak-password")) {
            return "비밀번호가 너무 약합니다. 6자 이상 입력해주세요.";
        }
        if (raw.contains("recent login")) {
            return "보안을 위해 다시 로그인해야 합니다.";
        }
        if (raw.contains("too many failed")) {
            return "시도 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요.";
        }
        if (raw.contains("network error")) {
            return "네트워크 연결을 확인해주세요.";
        }

        return "오류가 발생했습니다: " + e.getMessage();
    }
}