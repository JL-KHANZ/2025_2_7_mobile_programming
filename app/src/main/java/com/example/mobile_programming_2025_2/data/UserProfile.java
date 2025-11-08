package com.example.mobile_programming_2025_2.data;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
public class UserProfile {
    public String uid;
    public String name;        // 실명
    public String nickname;    // 닉네임
    public String email;
    public String birthdate;   // "YYYY-MM-DD"
    public String photoURL;    // Firebase Storage downloadURL (프로필사진)

    @ServerTimestamp public Date createdAt;  // 최초 생성 시 자동
    @ServerTimestamp public Date updatedAt;  // 매 저장 시 자동

    public UserProfile() {} // Firestore 역직렬화용 기본 생성자

    public UserProfile(String uid, String name, String nickname,
                       String email, String birthdate, String photoURL) {
        this.uid = uid;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.birthdate = birthdate;
        this.photoURL = photoURL;
    }
}
