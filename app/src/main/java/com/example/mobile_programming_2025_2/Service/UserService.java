package com.example.mobile_programming_2025_2.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.ProfileUpdate;
import com.example.mobile_programming_2025_2.data.ServiceResult;
import com.example.mobile_programming_2025_2.data.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
//   profile                         ← (collection)
//        └─ info                        ← (document)  ← 프로필 문서 저장 위치
//          ├─ uid: string   실제 uid 저장
//          ├─ email: string   실제 email 저장
//          ├─ name: string  기본값 “ “ 저장
//          ├─ nickname: string    기본값“ “ 저장
//          ├─ birthdate: "YYYY-MM-DD"    기본값“ “ 저장
//          ├─ photoURL: string   기본값으로 null    // Firebase Storage download URL
//          ├─ createdAt: Timestamp    // @ServerTimestamp
//          └─ updatedAt: Timestamp    // @ServerTimestamp

public class UserService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final String DEFAULT_NAME = "";
    private static final String DEFAULT_NICKNAME = "";
    private static final String DEFAULT_BIRTHDATE = "";
    // TODO: 실제 기본 이미지 URL로 교체하거나 null로 두고 UI에서 placeholder 사용
    private static final String DEFAULT_PHOTO_URL = null;
    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }
    private String docPath(String uid) {
        return "users/" + uid + "/profile/info";// users/{uid}/profile/info  (컬렉션 users, 문서 uid, 컬렉션 profile, 문서 info)
    }


    /** 현재 로그인된 사용자(Auth) 기반으로 기본 프로필 업서트 (회원가입 직후 호출) */
    public Task<Void> upsertProfileFromCurrentUser() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        String uid = currentUid();
        DocumentReference docRef = db.document(docPath(uid)); // users/{uid}/profile/info

        return docRef.get().continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    var snap = task.getResult();
                    Map<String, Object> toSet = new HashMap<>();

                    // uid, email은 사용자 회원가입시 값 가져옴
                    if (snap == null || !snap.exists() || !snap.contains("uid")) {
                        toSet.put("uid", uid);
                    }
                    if ((snap == null || !snap.contains("email")) && u.getEmail() != null) {
                        toSet.put("email", u.getEmail());
                    }

                    // 기본값 설정. 그냥 넣어도 되지만 추후 확장성 고려
                    if (snap == null || !snap.contains("name")) {
                        toSet.put("name", DEFAULT_NAME);
                    }
                    if (snap == null || !snap.contains("nickname")) {
                        toSet.put("nickname", DEFAULT_NICKNAME);
                    }
                    if (snap == null || !snap.contains("birthdate")) {
                        toSet.put("birthdate", DEFAULT_BIRTHDATE);
                    }

//                    if (snap == null || !snap.contains("photoURL")) {
//                        // toSet.put("photoURL", "https://your.cdn/default_avatar.png");
//                        // 또는 저장하지 않음(주석)
//                    }
                    // createdAt 없으면 생성(매번 갱신하면 안됨)
                    if (snap == null || !snap.contains("createdAt")) {
                        toSet.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    }
                    // updatedAt 매번 갱신
                    toSet.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    if (toSet.isEmpty()) {
                        // 이미 다 채워져 있으면 NO-OP
                        return Tasks.forResult(null);
                    }
                    return docRef.set(toSet, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnSuccessListener(v -> Log.d("UserService", "기본 프로필 업서트(기본값 보정) 완료: " + uid))
                .addOnFailureListener(e -> Log.e("UserService", "기본 프로필 업서트 실패", e));
    }


    /** 프로필 변경 사항 저장 */
    // name, nickname, birthdate, photoUri
    public Task<Void> saveMyProfile(@NonNull ProfileUpdate update) {
        String uid = currentUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        Map<String, Object> data = new HashMap<>();

        // 허용 필드만 수집 (화이트리스트)
        if (update.name != null)      data.put("name", update.name);
        if (update.nickname != null)  data.put("nickname", update.nickname);
        if (update.birthdate != null) data.put("birthdate", update.birthdate);

        // uid는 서버에서 강제
        data.put("uid", uid);

        DocumentReference doc = db.document(docPath(uid));

        // 사진이 없으면 갱신된 데이터들 Firestore에 바로 저장
        if (update.photoUri == null) {
            return doc.set(data, SetOptions.merge());
        }
        /** photoUri:로컬 이미지 경로  photoUrl:storage에 업로드후 생기는 이미지 주소 ->firestore에 저장해서 앱에서 불러올 수 있음*/
        // 로컬 uri -> storage -> url생성 ->url firestore에 저장
        // 사진 있으면 Firebase Storage 업로드 → URL → Firestore에 저장 + 갱신 데이터 저장
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

    /**
     * 회원가입과 동시에 uid, email은 값이 있음
     * name, nickname, birthdate는 "" 공백 값으로 초기화됨
     * photoUrl은 null로 초기화됨
     * 1. 기본 회원가입 상태에서 조회시 위 값들이 나올것임
     * 2. 정보가 갱신 된 상태면 갱신된 상태로 나올것임
     */
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

//========================================================================================
    /**
     * 비밀번호 변경  (recent login 보장 상태 가정)
     * Firebase Auth 계정의 비밀번호만 변경함
     * but 실제로 로그인한지 너무 오래되면 로직이 꼬일 수 있음
     * 총 2단계로 나뉨. recent login인 보장하기 위함 + 비밀번호 변경
     * 1. 현재 비밀번호 확인 + recent login 처리
     * 2. 입력받은 비밀번호로 변경
     * 각 단계에서 적절한 예외처리 필요
     * 1. 비밀번호가 틀렸습니다. 2. 적절하지 않은 비밀번호입니다. 비밀번호가 너무 짧습니다 등
     */
    public Task<ServiceResult> changePassword(@NonNull String currentPassword, @NonNull String newPassword) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(
                    new ServiceResult(false, "로그인이 필요합니다.")
            );
        }

        String email = user.getEmail();
        //소셜 로그인도 가능해지면 이 부분 수정 (email 없을 수 도 있음)
        //현재는 이메일 로그인만 상정하고있음
        if (email == null) {
            return Tasks.forResult(
                    new ServiceResult(false, "이메일 정보를 찾을 수 없습니다.")
            );
        }

        // 최소 검증 / 물론 authentication에서 검증 해주긴 하는데 혹시 몰라서
        if (newPassword.length() < 6) {
            return Tasks.forResult(
                    new ServiceResult(false, "새 비밀번호는 6자 이상이어야 합니다.")
            );
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(email, currentPassword);

        //  재인증 
        return user.reauthenticate(credential)
                .onSuccessTask(v -> user.updatePassword(newPassword))   //재인증 성고시 비밀번호 변경
                .continueWith(task -> {

                    if (task.isSuccessful()) {
                        return new ServiceResult(true, "비밀번호가 성공적으로 변경되었습니다.");//비밀번호 변경 성공시 serviceResult에 성공 메세지 담음
                    }

                    // 예외 → 사용자 메시지 변환
                    Exception e = task.getException();
                    String msg = convertError(e);
                    return new ServiceResult(false, msg);
                });
    }

    /**
    * Firebase Auth Exception → 사용자 메시지 변환
     * 실제 exception은 여러 케이스를 묶어서 발생하는데
     * 그걸 문자열을 나눠서 구분하는 방법(firebase 문서 추천)
     *  todo: 모든 케이스 테스트 해봐야함
    */
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

//========================================================================================
    /**
     * 요청에 따라 account 삭제
     * 삭제시 비밀번호 입력받아야함
     * 진짜 삭제할건지 검증
     * */
    //todo:계정 삭제 authentication, firestore, storage 모두 삭제
//    public Task<Void> deleteMyAccount() {}

}

