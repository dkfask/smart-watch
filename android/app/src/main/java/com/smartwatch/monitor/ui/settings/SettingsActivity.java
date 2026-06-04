package com.smartwatch.monitor.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.smartwatch.monitor.R;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.utils.Constants;

/**
 * 设置页面Activity，提供服务器地址、WebSocket地址、通知和暗色模式等配置
 */
public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etServerUrl;
    private TextInputEditText etWsUrl;
    private SwitchMaterial switchNotification;
    private SwitchMaterial switchDarkMode;
    private SharedPreferences prefs;

    /**
     * Activity创建时回调，初始化视图和加载设置
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initViews();
        loadSettings();

        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveSettings());
    }

    /**
     * 设置工具栏和返回按钮
     */
    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        etServerUrl = findViewById(R.id.et_server_url);
        etWsUrl = findViewById(R.id.et_ws_url);
        switchNotification = findViewById(R.id.switch_notification);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 从SharedPreferences加载已保存的设置
     */
    private void loadSettings() {
        String serverUrl = prefs.getString("server_url", Constants.BASE_URL);
        String wsUrl = prefs.getString("ws_url", Constants.DEFAULT_WS_URL);
        boolean notificationEnabled = prefs.getBoolean("notification_enabled", true);
        boolean darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false);

        etServerUrl.setText(serverUrl);
        etWsUrl.setText(wsUrl);
        switchNotification.setChecked(notificationEnabled);
        switchDarkMode.setChecked(darkModeEnabled);
    }

    /**
     * 保存设置到SharedPreferences，服务器地址变更时重建ApiClient
     */
    private void saveSettings() {
        String newServerUrl = etServerUrl.getText() != null ? etServerUrl.getText().toString().trim() : "";
        String newWsUrl = etWsUrl.getText() != null ? etWsUrl.getText().toString().trim() : "";
        boolean notificationEnabled = switchNotification.isChecked();
        boolean darkModeEnabled = switchDarkMode.isChecked();

        if (newServerUrl.isEmpty()) {
            etServerUrl.setError("服务器地址不能为空");
            return;
        }

        String oldServerUrl = prefs.getString("server_url", Constants.BASE_URL);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("server_url", newServerUrl);
        editor.putString("ws_url", newWsUrl);
        editor.putBoolean("notification_enabled", notificationEnabled);
        editor.putBoolean("dark_mode_enabled", darkModeEnabled);
        editor.apply();

        if (!newServerUrl.equals(oldServerUrl)) {
            recreateApiClient(newServerUrl);
        }

        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * 重建ApiClient实例，使用新的服务器地址
     * @param newBaseUrl 新的服务器基础地址
     */
    private void recreateApiClient(String newBaseUrl) {
        ApiClient.getInstance().init(this, newBaseUrl);
    }
}
