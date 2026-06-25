package com.example.itstore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.itstore.R;

import com.example.itstore.databinding.ActivityLoginBinding;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.utils.SharedPrefsManager;
import com.example.itstore.viewmodel.LoginViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel loginViewModel;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        

        // Chuyển tới trang Đăng ký
        binding.tvRegister.setOnClickListener((v) -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        // Chuyển tới trang Quên mật khẩu
        binding.tvForgotPassword.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });


        // Lắng nghe và xử lí sự kiện khi nhập email và mật khẩu
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupObservers();

        // Xử lí sự kiện khi nhấn nút Đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.edtEmail.getText().toString().trim();
            String passwd = binding.edtPassword.getText().toString().trim();
            loginViewModel.login(email, passwd);
        });


        // Đăng nhập bằng GG
        String webClienId = "777572620781-37ul02o28fcbl8acm4l708so3n5q48da.apps.googleusercontent.com";
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClienId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        binding.btnGoogleLogin.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });

        binding.tvGuest.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        String idToken = account.getIdToken();

                        if (idToken != null) {
                            android.util.Log.d("DEBUG_GG_TOKEN", "Token: " + idToken);
                            loginViewModel.googleLogin(idToken);
                        }
                    }
                    catch (ApiException e) {
                        Toast.makeText(this, "Xác thực Google thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
//                      Log.e("DEBUG_GG", "Lỗi Google Sign In: " + e.getStatusCode());
//                      Toast.makeText(this, "Mã lỗi GG: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private void setupObservers() {
        loginViewModel.getEmailError().observe(this, errorMessage -> {
            if (errorMessage != null){
                binding.tilEmail.setError(errorMessage);
                binding.tilEmail.requestFocus();
            } else {
                binding.tilEmail.setErrorEnabled(false);
            }
        });


        loginViewModel.getPasswordError().observe(this, errorMessage -> {
            if (errorMessage != null){
                binding.tilPassword.setError(errorMessage);
                binding.tilPassword.requestFocus();
            } else {
                binding.tilPassword.setErrorEnabled(false);
            }
        }
        );

        loginViewModel.getIsLoading().observe(this, isLoading ->{
            if (isLoading) {
                binding.btnLogin.setEnabled(false);
                binding.btnLogin.setText("Đang đăng nhập...");
            } else {
                binding.btnLogin.setEnabled(true);
                binding.btnLogin.setText("Đăng nhập");
            }
        });

        loginViewModel.getApiError().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        loginViewModel.getLoginSuccessData().observe(this, data -> {
            if (data != null) {
                SharedPrefsManager.getInstance(this).saveTokens(
                        data.getAccessToken(),
                        data.getRefreshToken()
                );

                SharedPrefsManager.getInstance(this).saveUserInfo(
                        data.getUser().getId(),
                        data.getUser().getFullName(),
                        data.getUser().getEmail(),
                        data.getUser().getRole()
                );

                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        loginViewModel.getUnverifiedEmailSuccess().observe(this, email -> {
            if (email != null) {
                Toast.makeText(this, "Tài khoản chưa xác thực. Đã gửi mã kích hoạt vào email của bạn!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(LoginActivity.this, VerifyOtpActivity.class);
                intent.putExtra("USER_EMAIL", email);
                startActivity(intent);
            }
        });

        loginViewModel.getUnverifiedEmailFailed().observe(this, email -> {
            if (email != null) {
                Toast.makeText(this, "Hệ thống đang nghẽn. Vui lòng tự bấm Gửi lại mã!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(LoginActivity.this, VerifyOtpActivity.class);
                intent.putExtra("USER_EMAIL", email);
                intent.putExtra("EMAIL_FAILED", true);
                startActivity(intent);
            }
        });
    }
}