package com.example.mobile_programming_2025_2.data;

import android.net.Uri;

public class ProfileUpdate {
    public String name;       // null이면 변경 안 함
    public String nickname;
    public String birthdate;  // "YYYY-MM-DD"
    public Uri photoUri;
    public String email;
}