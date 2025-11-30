package com.example.mobile_programming_2025_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.Service.SignUpService;
import com.example.mobile_programming_2025_2.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.btnSignUp.setOnClickListener(v -> doSignUp());
    }

    private void doSignUp() {
        String name = b.etName.getText().toString().trim();
        String email = b.etEmail.getText().toString().trim();
        String pwd = b.etPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pwd)) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pwd.length() < 6) {
            Toast.makeText(this, "비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        setUiEnabled(false);

        SignUpService signUpService = new SignUpService();
        signUpService.signUp(email, pwd, name, resultCode -> {
            setUiEnabled(true);

            switch (resultCode) {
                case 0:
                    // 성공!
                    Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show();
                    Log.d("SignUpActivity", "회원가입 및 프로필 생성 성공");

                    // 바로 메인 화면으로 이동 (로그인 유지 설정 포함)
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    break;

                case -1:
                    Toast.makeText(this, "이메일 형식이 올바르지 않거나 이미 존재하는 계정입니다.", Toast.LENGTH_SHORT).show();
                    break;
                case -2:
                    Toast.makeText(this, "잘못된 계정 정보입니다.", Toast.LENGTH_SHORT).show();
                    break;
                case -3:
                    Toast.makeText(this, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUiEnabled(boolean enabled) {
        b.btnSignUp.setEnabled(enabled);
        b.etName.setEnabled(enabled);
        b.etEmail.setEnabled(enabled);
        b.etPassword.setEnabled(enabled);
    }
}