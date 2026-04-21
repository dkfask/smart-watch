package com.smartwatch.monitor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.service.WebSocketManager;
import com.smartwatch.monitor.utils.AlarmNotificationHelper;
import com.smartwatch.monitor.utils.Constants;

/**
 * 应用程序入口类，负责全局初始化
 */
public class SmartWatchApp extends Application {

    /**
     * 应用创建时回调，初始化ApiClient单例和通知渠道
     * 从SharedPreferences读取用户配置的服务器地址
     */
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        String savedServerUrl = prefs.getString("server_url", Constants.BASE_URL);
        ApiClient.getInstance().init(this, savedServerUrl);
        AlarmNotificationHelper.createNotificationChannel(this);
    }
}
