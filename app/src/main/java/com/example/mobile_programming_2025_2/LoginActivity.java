package com.example.mobile_programming_2025_2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_programming_2025_2.Service.LoginService;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
//        if (/* already logged in, e.g., valid token */) {
//            startActivity(new Intent(this, MainActivity.class)
//                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            return;
//        }
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pwd   = etPassword.getText().toString();

            // TODO: validate credentials / call API
            LoginService authService = new LoginService();
            authService.login(email, pwd, resultCode -> {
            System.out.println("resultCode =>" + resultCode);
                switch (resultCode) {
                    case 0:
                        // 로그인 성공
                        startActivity(new Intent(this, MainActivity.class));
                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        break;
                    case -1:
                        Toast.makeText(this, "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    case -2:
                        Toast.makeText(this, "등록되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    case -4:
                        Toast.makeText(this, "이메일,비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    default:
                        Toast.makeText(this, "로그인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
            });






        });

        //회원가입
        TextView go = findViewById(R.id.tvGoSignUp);
        if (go != null) {
            go.setOnClickListener(v ->
                    startActivity(new Intent(this, SignUpActivity.class)));
        }

    }
}
