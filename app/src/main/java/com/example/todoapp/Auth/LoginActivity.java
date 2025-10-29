package com.example.todoapp.Auth;



import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todoapp.MainActivity;
import com.example.todoapp.R;
import com.example.todoapp.Repository.FirebaseAuthRepository;
import com.facebook.CallbackManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnFacebookLogin;
    private CallbackManager callbackManager;
    private FirebaseAuthRepository authRepository;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize Facebook SDK
        callbackManager = CallbackManager.Factory.create();
        authRepository = new FirebaseAuthRepository();

    // Lưu trạng thái đăng nhập

//        if (authRepository.getCurrentUser() != null) {
//            // ✅ Đã đăng nhập → sang Main luôn, không cần Login lại
//            startActivity(new Intent(this, MainActivity.class));
//            finish(); // Đóng LoginActivity
//            return;
//        }


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
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Sign up link click
        findViewById(R.id.tvSignUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        //Quên mật khẩu
//        findViewById(R.id.tvForgotPassword).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                EditText input = new EditText(LoginActivity.this);
//                input.setHint("Nhập email của bạn");
//
//                new AlertDialog.Builder(LoginActivity.this)
//                        .setTitle("Quên mật khẩu")
//                        .setMessage("Nhập email để nhận liên kết đặt lại mật khẩu")
//                        .setView(input)
//                        .setPositiveButton("Gửi", (dialog, which) -> {
//                            String email = input.getText().toString().trim();
//                            if (email.isEmpty()) {
//                                Toast.makeText(LoginActivity.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
//                                return;
//                            }
//
//                            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
//                                    .addOnCompleteListener(task -> {
//                                        if (task.isSuccessful()) {
//                                            Toast.makeText(LoginActivity.this, "Đã gửi email khôi phục đến " + email, Toast.LENGTH_LONG).show();
//                                        } else {
//                                            Toast.makeText(LoginActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                                        }
//                                    });
//                        })
//                        .setNegativeButton("Hủy", null)
//                        .show();
//            }
//        });


        // Facebook login button click
        btnFacebookLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Facebook login
                loginWithFacebook();
            }
        });
    }

    private void setupFacebookLogin() {
        // Facebook login callback
        // Note: You'll need to add Facebook SDK dependency and configure it
        /*
        LoginButton loginButton = new LoginButton(this);
        loginButton.setPermissions("email");

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Handle successful Facebook login
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Đăng nhập Facebook bị hủy", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(LoginActivity.this, "Lỗi đăng nhập Facebook: " + exception.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (!validateInput(email, password)) {
            return;
        }

        // Show loading
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // 🔹 Gọi Firebase Auth để đăng nhập thật
        authRepository.login(email, password)
                .addOnSuccessListener(result -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang trang chính (MainActivity)
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String email, String password) {
        // Check email/username
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email hoặc tên đăng nhập");
            etEmail.requestFocus();
            return false;
        }

        // If it contains @, validate as email
        if (email.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        // Check password
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

    private void simulateLogin(String email, String password) {
        // Simulate network delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Reset button
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");

                // For demo purposes, accept any valid input
                if (email.equals("admin@example.com") && password.equals("123456")) {
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    // Navigate to main activity
                    // startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    // finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Email/tên đăng nhập hoặc mật khẩu không đúng",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }, 2000);
    }

    private void loginWithFacebook() {
        // TODO: Implement Facebook login
        Toast.makeText(this, "Chức năng đăng nhập Facebook sẽ được triển khai", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}