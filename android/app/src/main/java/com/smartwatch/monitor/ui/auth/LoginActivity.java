package com.smartwatch.monitor.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.ui.MainActivity;

/**
 * 登录Activity，处理用户登录界面和逻辑
 */
public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    /**
     * Activity创建时回调，初始化视图和ViewModel
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initViewModel();
        checkLoginState();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(v -> performLogin());
    }

    /**
     * 初始化ViewModel并观察数据变化
     */
    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        authViewModel.getLoginResult().observe(this, success -> {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            if (success != null && success) {
                navigateToMain();
            } else {
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 检查是否已登录，如果已登录则直接跳转主界面
     */
    private void checkLoginState() {
        if (authViewModel.isLoggedIn()) {
            navigateToMain();
        }
    }

    /**
     * 执行登录操作
     */
    private void performLogin() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.please_input_username_password, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        authViewModel.login(username, password);
    }

    /**
     * 跳转到主界面
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
