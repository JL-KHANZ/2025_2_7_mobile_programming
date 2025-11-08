package com.example.mobile_programming_2025_2.Service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mobile_programming_2025_2.data.ProfileUpdate;
import com.example.mobile_programming_2025_2.data.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class UserService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private String currentUid() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }
    private String docPath(String uid) {
        return "users/" + uid + "/profile";// users/{uid}/profile  (컬렉션 users, 문서 uid, 하위 profile 문서 1개)
    }


    /** 현재 로그인된 사용자(Auth) 기반으로 기본 프로필 업서트 (회원가입 직후 호출) */
    public Task<Void> upsertProfileFromCurrentUser() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            // 로그인 안 된 상태면 바로 실패 Task 반환
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        String uid = u.getUid();
        Map<String, Object> base = new HashMap<>();
        base.put("uid", uid);
        base.put("email", u.getEmail());

        // Firestore 문서 경로
        DocumentReference docRef = db.document(docPath(uid));

        // Firestore에 기본 데이터 생성 (없으면 생성, 있으면 병합)
        return docRef.set(base, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    //성공 로그
                    Log.d("UserService", "기본 프로필 생성/병합 완료: " + uid);
                })
                .addOnFailureListener(e -> {
                    //실패 로그
                    Log.e("UserService", "기본 프로필 생성 실패", e);
                });
    }



    /** 프로필 변경 사항 저장 */
    // name, nickname, birthdate, photoUri
    public Task<Void> saveMyProfile(@NonNull ProfileUpdate update) {
        String uid = currentUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        FirebaseUser authUser = auth.getCurrentUser();
        Map<String, Object> data = new HashMap<>();

        // 허용 필드만 수집 (화이트리스트)
        if (update.name != null)      data.put("name", update.name);
        if (update.nickname != null)  data.put("nickname", update.nickname);
        if (update.birthdate != null) data.put("birthdate", update.birthdate);

        // uid는 서버에서 강제
        data.put("uid", uid);

//        // email: AuthUser에 있으면 우선 저장, 없으면 클라이언트 입력 허용
//        if (authUser != null && authUser.getEmail() != null) {
//            data.put("email", authUser.getEmail());
//        } else if (update.email != null) {
//            data.put("email", update.email);
//        }

        DocumentReference doc = db.document(docPath(uid));

        // 사진이 없으면 갱신된 데이터들 Firestore에 바로 저장
        if (update.photoUri == null) {
            return doc.set(data, SetOptions.merge());
        }

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

}

