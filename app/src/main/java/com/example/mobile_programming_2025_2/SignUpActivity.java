package com.example.mobile_programming_2025_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.databinding.ActivitySignUpBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding b;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();

        b.btnSignUp.setOnClickListener(v -> doSignUp());
    }

    private void doSignUp() {
        String email = b.etEmail.getText().toString().trim();
        String pwd   = b.etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd)) {
            Toast.makeText(this, "이메일/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        setUiEnabled(false);

        auth.createUserWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(task -> {
                    setUiEnabled(true);

                    if (!task.isSuccessful()) {
                        Toast.makeText(this, parse(task.getException()), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // (선택) 이메일 인증 메일 발송
//                    FirebaseUser u = auth.getCurrentUser();
//                    if (u != null) {
//                        // u.sendEmailVerification(); // 인증 메일을 강제하고 싶으면 주석 해제
//                    }

                    Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show();
                    finish(); // 로그인 화면으로 복귀 → 이미 로그인된 상태라면 로그인 화면에서 자동 진입 처리됨
                });
    }

    private void setUiEnabled(boolean enabled) {
        b.btnSignUp.setEnabled(enabled);
        b.etEmail.setEnabled(enabled);
        b.etPassword.setEnabled(enabled);
    }

    private String parse(Exception e) {
        if (e == null) return "회원가입 실패";
        if (e instanceof FirebaseAuthWeakPasswordException)        return "비밀번호가 너무 약합니다(6자 이상).";
        if (e instanceof FirebaseAuthInvalidCredentialsException)  return "이메일 형식이 올바르지 않습니다.";
        if (e instanceof FirebaseAuthUserCollisionException)       return "이미 등록된 계정입니다.";
        return e.getMessage();
    }
}
