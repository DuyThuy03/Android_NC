package com.example.todoapp.Auth;



import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todoapp.R;
import com.example.todoapp.Repository.FirebaseAuthRepository;
import com.facebook.CallbackManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etGmail, etUsername, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnFacebookRegister;
    private CheckBox cbTerms;
    private CallbackManager callbackManager;
    private FirebaseAuthRepository authRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        // Initialize Facebook SDK
        callbackManager = CallbackManager.Factory.create();

        initViews();
        setupClickListeners();
        setupFacebookRegister();
        authRepository = new FirebaseAuthRepository();

    }

    private void initViews() {
        etGmail = findViewById(R.id.etGmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnFacebookRegister = findViewById(R.id.btnFacebookRegister);
        cbTerms = findViewById(R.id.cbTerms);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Register button click
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Login link click
        findViewById(R.id.tvLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to login
            }
        });

        // Facebook register button click
        btnFacebookRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerWithFacebook();
            }
        });
    }

    private void setupFacebookRegister() {
        // Facebook register callback - similar to login
        // Note: You'll need to add Facebook SDK dependency and configure it
    }

    private void registerUser() {
        String gmail = etGmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!validateInput(gmail, username, password, confirmPassword)) {
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với điều khoản sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang tạo tài khoản...");

        // 🔹 Gọi Firebase đăng ký thật
        authRepository.register(gmail, password, username)
                .addOnSuccessListener(result -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("TẠO TÀI KHOẢN");

                    Toast.makeText(this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển về trang đăng nhập
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("TẠO TÀI KHOẢN");
                    Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private boolean validateInput(String gmail, String username, String password, String confirmPassword) {
        // Check Gmail
        if (TextUtils.isEmpty(gmail)) {
            etGmail.setError("Vui lòng nhập địa chỉ Gmail");
            etGmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(gmail).matches()) {
            etGmail.setError("Địa chỉ Gmail không hợp lệ");
            etGmail.requestFocus();
            return false;
        }

        if (!gmail.endsWith("@gmail.com")) {
            etGmail.setError("Vui lòng sử dụng địa chỉ Gmail (kết thúc bằng @gmail.com)");
            etGmail.requestFocus();
            return false;
        }

        // Check username
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Vui lòng nhập tên đăng nhập");
            etUsername.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            etUsername.setError("Tên đăng nhập phải có ít nhất 3 ký tự");
            etUsername.requestFocus();
            return false;
        }

        // Check if username contains spaces
        if (username.contains(" ")) {
            etUsername.setError("Tên đăng nhập không được chứa khoảng trắng");
            etUsername.requestFocus();
            return false;
        }

        // Check password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 3) {
            etPassword.setError("Mật khẩu phải có ít nhất 3 ký tự");
            etPassword.requestFocus();
            return false;
        }

        // Check password strength
//        if (!isPasswordStrong(password)) {
//            etPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ cái và 1 số");
//            etPassword.requestFocus();
//            return false;
//        }

        // Check confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

//    private boolean isPasswordStrong(String password) {
//        // Check if password contains at least one letter and one digit
//        boolean hasLetter = password.matches(".*[a-zA-Z].*");
//        boolean hasDigit = password.matches(".*[0-9].*");
//        return hasLetter && hasDigit;
//    }

    private void simulateRegister(String fullName, String username, String password) {
        // Simulate network delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Reset button
                btnRegister.setEnabled(true);
                btnRegister.setText("TẠO TÀI KHOẢN");

                // For demo purposes, always success
                Toast.makeText(RegisterActivity.this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();

                // Navigate back to login
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    private void registerWithFacebook() {
        // TODO: Implement Facebook registration
        Toast.makeText(this, "Chức năng đăng ký Facebook sẽ được triển khai", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
