package com.example.mobile_programming_2025_2.Service;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;


public class SignUpService {
    public interface SignUpCallback {
        void onResult(int resultCode);
    }

    private final FirebaseAuth auth;

    // 생성자 (리턴타입 없음)
    public SignUpService() {
        this.auth = FirebaseAuth.getInstance();
    }


    public void signUp(String email, String pwd, SignUpCallback callback) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd)) {
            callback.onResult(-4);
            return;
        }

        auth.createUserWithEmailAndPassword(email, pwd)
                 .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                System.out.println("onComplete call....");

                                if (task.isSuccessful()) {

                                    System.out.println("isSuccessful call....");

                                    callback.onResult(0);
                                    return;
                                }

                                System.out.println("isSuccessful fail....");
                                Exception e = task.getException();
                                System.out.println("isSuccessful fail...."+e.getMessage());

                                if (e instanceof FirebaseAuthInvalidUserException) {
                                    // 이메일 없음 or 탈퇴 계정
                                    callback.onResult(-2);
                                    System.out.println("isSuccessful fail. (-2)...");

                                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {

                                    System.out.println("isSuccessful fail. (-1)...");
                                    // 비밀번호 불일치
                                    //이메일 형식이 올바르지 않거나 비밀번호가 너무 짧습니다.
                                    callback.onResult(-1);
                                } else {
                                    System.out.println("isSuccessful fail. (-3)..."+e.getMessage());

                                    callback.onResult(-3); // 기타 에러 (네트워크 등)
                                }
                            }
                        });


    }
}
