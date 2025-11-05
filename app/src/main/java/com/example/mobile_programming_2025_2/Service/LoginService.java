package com.example.mobile_programming_2025_2.Service;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
public class LoginService {
    private FirebaseAuth auth;
    public interface LoginCallback {
        void onResult(int resultCode);
    }
    public LoginService() {
        this.auth = FirebaseAuth.getInstance();
    }

    public void login(String email, String password, LoginCallback callback) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            callback.onResult(-4); // 이메일 또는 비밀번호 없음
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        System.out.println("onCom");
                        if (task.isSuccessful()) {
                            callback.onResult(0);
                            return;
                        }

                        Exception e = task.getException();
                        System.out.println("isSuccessful fail...."+e.getMessage());
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            System.out.println("isSuccessful fail-2...."+e.getMessage());
                            // 이메일 없음 or 탈퇴 계정
                            callback.onResult(-2);
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            System.out.println("isSuccessful fail(-1)...."+e.getMessage());
                            //이메일 형식이 올바르지 않거나 비밀번호가 너무 짧습니다.
                            callback.onResult(-1);
                        } else {
                            System.out.println("isSuccessful fail.-3..."+e.getMessage());
                            callback.onResult(-3); // 기타 에러 (네트워크 등)
                        }
                    }
                });
    }
}
