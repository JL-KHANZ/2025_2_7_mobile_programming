package com.example.mobile_programming_2025_2.Service;

import android.text.TextUtils;
import androidx.annotation.NonNull;

import com.example.mobile_programming_2025_2.data.UserProfile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpService {
    public interface SignUpCallback {
        void onResult(int resultCode);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public SignUpService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void signUp(String email, String pwd, String name, SignUpCallback callback) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd) || TextUtils.isEmpty(name)) {
            callback.onResult(-4);
            return;
        }

        auth.createUserWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                // (uid, name, nickname, email, birthdate, photoURL)
                                // 닉네임(name)을 nickname 필드에 저장
                                UserProfile newProfile = new UserProfile(uid, null, name, email, null, null);

                                // ⭐️ [핵심] 저장 경로를 UserService가 읽는 곳과 일치시킴!
                                String path = "users/" + uid + "/profile/info";

                                db.document(path).set(newProfile)
                                        .addOnSuccessListener(aVoid -> {
                                            callback.onResult(0);
                                        })
                                        .addOnFailureListener(e -> {
                                            callback.onResult(-3);
                                        });
                            } else {
                                callback.onResult(-3);
                            }
                            return;
                        }

                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            callback.onResult(-2);
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            callback.onResult(-1);
                        } else {
                            callback.onResult(-3);
                        }
                    }
                });
    }
}