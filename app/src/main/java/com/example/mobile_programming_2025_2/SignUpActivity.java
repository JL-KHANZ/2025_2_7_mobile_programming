package com.example.mobile_programming_2025_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.Service.SignUpService;
import com.example.mobile_programming_2025_2.Service.UserService;
import com.example.mobile_programming_2025_2.databinding.ActivitySignUpBinding;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding b;
    private FirebaseAuth auth;
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.btnSignUp.setOnClickListener(v -> doSignUp());
    }

    private void doSignUp() {
        String email = b.etEmail.getText().toString().trim();
        String pwd   = b.etPassword.getText().toString();
//        setUiEnabled(false);
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd)) {
            Toast.makeText(this, "이메일/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: validate credentials / call API
        SignUpService signUpService = new SignUpService();
        signUpService.signUp(email, pwd, resultCode -> {
            System.out.println("resultCode =>" + resultCode);
            switch (resultCode) {


                case 0:
                    // 로그인 성공
                    setUiEnabled(true);
                    Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show();

                    // Firestore 기본 프로필 자동 생성
                    UserService userService = new UserService();
                    userService.upsertProfileFromCurrentUser()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("LoginActivity", "기본 프로필 생성 성공");

                                // Firestore 업서트 성공 후 MainActivity로 이동
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("LoginActivity", "기본 프로필 생성 실패", e);
                                Toast.makeText(this, "기본 프로필 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                    break;
                case -1:
                    Toast.makeText(this, "이메일 형식이 올바르지 않거나 비밀번호가 너무 짧습니다.", Toast.LENGTH_SHORT).show();
                    break;
                case -2:
                    Toast.makeText(this, "잘못된 계정입니다.", Toast.LENGTH_SHORT).show();
                    break;
                case -3:
                    Toast.makeText(this, "등록 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    break;
                case -4:
                    Toast.makeText(this, "이메일/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "등록 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();


            }
        });




    }

    private void setUiEnabled(boolean enabled) {
        b.btnSignUp.setEnabled(enabled);
        b.etEmail.setEnabled(enabled);
        b.etPassword.setEnabled(enabled);
    }


}
