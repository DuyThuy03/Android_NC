package com.example.todoapp.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todoapp.MainActivity;
import com.example.todoapp.R;
import com.example.todoapp.Repository.FirebaseAuthRepository;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.example.todoapp.model.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnFacebookLogin;
    private CallbackManager callbackManager;
    private FirebaseAuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize Facebook SDK & CallbackManager
        callbackManager = CallbackManager.Factory.create();
        authRepository = new FirebaseAuthRepository();

        // Kiểm tra trạng thái đăng nhập
        if (authRepository.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        setupFacebookLogin();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnFacebookLogin = findViewById(R.id.btnFacebookLogin);
    }

    private void setupClickListeners() {
        // Login button click
        btnLogin.setOnClickListener(v -> loginUser());

        // Sign up link click
        findViewById(R.id.tvSignUp).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        // Quên mật khẩu
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        // Facebook login button click
        btnFacebookLogin.setOnClickListener(v -> loginWithFacebook());
    }

    private void setupFacebookLogin() {
        // Đăng ký callback cho Facebook Login
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "facebook:onCancel");
                        Toast.makeText(LoginActivity.this,
                                "Đăng nhập Facebook bị hủy",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "facebook:onError", error);
                        Toast.makeText(LoginActivity.this,
                                "Lỗi đăng nhập Facebook: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void loginWithFacebook() {
        // Hiển thị loading
        btnFacebookLogin.setEnabled(false);
        btnFacebookLogin.setText("Đang kết nối...");

        // Yêu cầu quyền email và public_profile
        LoginManager.getInstance().logInWithReadPermissions(
                this,
                Arrays.asList("email", "public_profile")
        );
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        // Đăng nhập Firebase với Facebook credential
        authRepository.loginWithFacebook(token)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "signInWithCredential:success");

                    // ✅ Lưu thông tin user vào Firestore
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null) {
                        saveUserToFirestore(firebaseUser);
                    }

                    // Reset button state
                    btnFacebookLogin.setEnabled(true);
                    btnFacebookLogin.setText("Đăng nhập bằng Facebook");

                    Toast.makeText(LoginActivity.this,
                            "Đăng nhập Facebook thành công!",
                            Toast.LENGTH_SHORT).show();

                    // Chuyển sang MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "signInWithCredential:failure", e);

                    // Reset button state
                    btnFacebookLogin.setEnabled(true);
                    btnFacebookLogin.setText("Đăng nhập bằng Facebook");

                    Toast.makeText(LoginActivity.this,
                            "Đăng nhập Firebase thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }


    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        // Show loading
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        authRepository.login(email, password)
                .addOnSuccessListener(result -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");

                    Toast.makeText(LoginActivity.this,
                            "Đăng nhập thành công!",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");
                    Toast.makeText(LoginActivity.this,
                            "Đăng nhập thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(LoginActivity.this);
        input.setHint("Nhập email của bạn");

        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Quên mật khẩu")
                .setMessage("Nhập email để nhận liên kết đặt lại mật khẩu")
                .setView(input)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(LoginActivity.this,
                                "Vui lòng nhập email",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this,
                                            "Đã gửi email khôi phục đến " + email,
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Lỗi: " + task.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email hoặc tên đăng nhập");
            etEmail.requestFocus();
            return false;
        }

        if (email.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }
    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String uid = firebaseUser.getUid();
        String username = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();

        // Tạo đối tượng User
        User user = new User(
                uid,
                username != null ? username : "Người dùng Facebook",
                email != null ? email : "Không có email",
                System.currentTimeMillis(),
                null // Mật khẩu null vì login bằng Facebook
        );

        // Kiểm tra user đã tồn tại chưa
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Nếu chưa có -> tạo mới
                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ User lưu Firestore thành công"))
                                .addOnFailureListener(e -> Log.w(TAG, "❌ Lưu user thất bại", e));
                    } else {
                        Log.d(TAG, "ℹ️ User đã tồn tại trong Firestore");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "🔥 Lỗi khi kiểm tra user", e));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}