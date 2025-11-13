package com.example.mobile_programming_2025_2.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_programming_2025_2.data.ProfileUpdate;
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

    /**
     * 이메일 및 비밀번호 변경 ( 비밀번호의 경우는 암호화 되어야 하기 때문에
     * 따로 저장해 두지 않고 그때그때 authentication에서 비교함)
     * uid는 건들지 않음
     */
    //todo: 한번 비밀번호 입력해야지 설정창 들어가지게
    // 로그인한지 너무 오래되면 로직이 꼬일 수 있음

    //    * 최근 로그인 충족을 위한 재인증
    //    - 보안 민감 작업(이메일/비밀번호 변경)에 필요할 수 있음
    //    - 화면에서 기존 이메일/비밀번호를 입력받아 호출하는 것을 권장
//    public Task<Void> reauthenticate(@NonNull String currentEmail, @NonNull String currentPassword) {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            return Tasks.forException(new IllegalStateException("Not signed in"));
//        }
//        AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, currentPassword);    //비밀번호 자격 증명 생성
//        return user.reauthenticate(credential)
//                .addOnSuccessListener(v -> Log.d("UserService", "Reauthenticate OK"))
//                .addOnFailureListener(e -> Log.e("UserService", "Reauthenticate FAIL", e));
//    }
//    public Task<Void> changePasswordWithReauth(@NonNull String currentEmail,
//                                               @NonNull String currentPassword,
//                                               @NonNull String newPassword) {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) return Tasks.forException(new IllegalStateException("Not signed in"));
//        if (newPassword.length() < 6) {
//            return Tasks.forException(new IllegalArgumentException("비밀번호는 6자 이상이어야 합니다."));
//        }
//
//        return reauthenticate(currentEmail, currentPassword)
//                .onSuccessTask(v -> user.updatePassword(newPassword))
//                .addOnSuccessListener(v -> Log.d("UserService", "Password changed after reauth"))
//                .addOnFailureListener(e -> Log.e("UserService", "changePasswordWithReauth failed", e));
//    }






//=========================================================================================================Email
    // updateEmail은 더이상 권장되지 않음.
    //    * 새 이메일로 '검증 메일' 보내기 (verifyBeforeUpdateEmail)
    //    - 사용자가 메일의 링크를 클릭해 소유를 증명하면, Auth의 이메일이 실제로 변경됨
    //    - locale: 검증 메일 언어코드(예: "ko") - null이면 기본
    //    - acs(ActionCodeSettings): 검증 메일 동작 옵션 - null이면 기본값으로 구성해서 사용
//    public Task<Void> requestEmailChangeWithVerification(@NonNull String newEmail,
//                                                         @Nullable String locale,
//                                                         @Nullable ActionCodeSettings acs) {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            return Tasks.forException(new IllegalStateException("Not signed in"));
//        }
//
//        // (선택) 검증 메일 언어 설정
//        if (locale != null) {
//            auth.setLanguageCode(locale);
//        }
//
//        // 기본 ActionCodeSettings (필요 시 프로젝트에 맞게 조정)
//        if (acs == null) {
//            acs = new ActionCodeSettings.Builder()
//                    // 검증 완료 후 돌아올 패키지 (안드로이드 앱 패키지)
//                    .setAndroidPackageName("com.example.mobile_programming_2025_2", true, null)
//                    // 앱 내에서 코드 처리할지 여부 (false여도 브라우저에서 처리 가능)
//                    .setHandleCodeInApp(false)
//                    // 필요 시 URL을 설정하여 웹 리디렉션 가능 (.setUrl("https://example.com/finishEmailChange"))
//                    .build();
//        }
//
//        // (중요) 재인증이 오래되면 RecentLoginRequired 예외가 발생할 수 있음 → 위 reauthenticate 먼저 수행 권장
//        return user.verifyBeforeUpdateEmail(newEmail, acs)
//                .addOnSuccessListener(v -> Log.d("UserService", "Verification email sent to: " + newEmail))
//                .addOnFailureListener(e -> {
//                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
//                        Log.e("UserService", "Recent login required. Reauthenticate first.", e);
//                    } else {
//                        Log.e("UserService", "verifyBeforeUpdateEmail failed", e);
//                    }
//                });
//    }


    //   * 사용자가 이메일 링크를 클릭해 검증을 완료한 뒤,
    //    -앱 복귀 시 Auth의 사용자 정보를 새로고침(reload)하고
    //    -Firestore(users/{uid}/profile/info)의 email 필드도 동기화
//    public Task<Void> syncEmailFromAuthToFirestore() {
//        FirebaseUser user = auth.getCurrentUser();
//        if (user == null) {
//            return Tasks.forException(new IllegalStateException("Not signed in"));
//        }
//
//        // 1) Auth 정보 최신화
//        return user.reload()
//                .continueWithTask(t -> {
//                    if (!t.isSuccessful()) throw t.getException();
//
//                    // 2) 최신 이메일을 Firestore에도 반영
//                    String uid = user.getUid();
//                    String email = user.getEmail(); // 검증 완료된 새 이메일
//                    DocumentReference doc = db.document(docPath(uid));
//
//                    Map<String, Object> up = new HashMap<>();
//                    up.put("email", email);
//                    up.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
//
//                    return doc.set(up, SetOptions.merge());
//                })
//                .addOnSuccessListener(v -> Log.d("UserService", "Firestore email synced from Auth"))
//                .addOnFailureListener(e -> Log.e("UserService", "Sync email to Firestore failed", e));
//    }



}

