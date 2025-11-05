package com.example.mobile_programming_2025_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText etEmail, etPassword;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

//        if (/* already logged in, e.g., valid token */) {
//            startActivity(new Intent(this, MainActivity.class)
//                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            return;
//        }
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            // TODO: validate credentials / call API
            String email = etEmail.getText().toString().trim();
            String pwd   = etPassword.getText().toString();
            //email or pwd empty일때
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pwd)) {
                Toast.makeText(this, "이메일/비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            v.setEnabled(false);

            auth.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(task -> {
                v.setEnabled(true);
                if (!task.isSuccessful()) {
                    Toast.makeText(this,
                            "로그인 실패: " + (task.getException()!=null? task.getException().getMessage():""),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
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
