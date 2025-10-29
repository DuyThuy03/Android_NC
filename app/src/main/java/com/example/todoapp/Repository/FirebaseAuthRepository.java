package com.example.todoapp.Repository;



import com.example.todoapp.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final String USER_COLLECTION = "users";

    public FirebaseAuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // 🔹 Đăng ký tài khoản mới
    public Task<AuthResult> register(String email, String password, String username) {
        return auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", uid);
                    userMap.put("username", username);
                    userMap.put("email", email);
                    userMap.put("createdAt", System.currentTimeMillis());

                    db.collection(USER_COLLECTION).document(uid).set(userMap);
                });
    }

    // 🔹 Đăng nhập
    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    // 🔹 Lấy thông tin người dùng hiện tại
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // 🔹 Đăng xuất
    public void logout() {
        auth.signOut();
    }

    // 🔹 Cập nhật tên hiển thị
    public Task<Void> updateUsername(String username) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build();
            return user.updateProfile(request);
        }
        return null;
    }

    // 🔹 Gửi email khôi phục mật khẩu
    public Task<Void> resetPassword(String email) {
        return auth.sendPasswordResetEmail(email);
    }
}
