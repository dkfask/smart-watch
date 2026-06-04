package com.smartwatch.monitor.service;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.smartwatch.monitor.api.ApiClient;
import com.smartwatch.monitor.model.Alert;
import com.smartwatch.monitor.model.DeviceLocation;
import com.smartwatch.monitor.model.DeviceStatus;
import com.smartwatch.monitor.utils.Constants;
import com.smartwatch.monitor.utils.SessionManager;
import java.util.Set;

/**
 * WebSocket管理器单例类，管理WebSocket生命周期
 * 提供设备订阅、连接状态和事件数据的LiveData观察
 */
public class WebSocketManager implements WebSocketService.WebSocketCallback {

    private static final String TAG = "WebSocketManager";

    /** WebSocket基础路径 */
    private static final String WS_PATH = "/ws";

    private static WebSocketManager instance;
    private WebSocketService webSocketService;
    private SessionManager sessionManager;
    private Context appContext;

    /** 连接状态LiveData */
    private final MutableLiveData<Boolean> connectionState = new MutableLiveData<>();

    /** 报警事件LiveData */
    private final MutableLiveData<Alert> alarmEvents = new MutableLiveData<>();

    /** 位置更新事件LiveData */
    private final MutableLiveData<DeviceLocation> locationEvents = new MutableLiveData<>();

    /** 设备状态更新事件LiveData */
    private final MutableLiveData<DeviceStatus> statusEvents = new MutableLiveData<>();

    /**
     * 获取WebSocketManager单例
     * @return WebSocketManager实例
     */
    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    /**
     * 私有构造函数
     */
    private WebSocketManager() {
    }

    /**
     * 启动WebSocket服务
     * @param context 应用上下文
     */
    public void start(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = ApiClient.getInstance().getSessionManager();

        String wsUrl = appContext
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getString("ws_url", Constants.DEFAULT_WS_URL);

        String cookie = buildCookieString();
        webSocketService = new WebSocketService(wsUrl, cookie);
        webSocketService.setCallback(this);
        webSocketService.connect();

        Log.d(TAG, "WebSocket服务已启动，连接地址: " + wsUrl);
    }

    /**
     * 停止WebSocket服务
     */
    public void stop() {
        if (webSocketService != null) {
            webSocketService.disconnect();
            webSocketService = null;
        }
        connectionState.setValue(false);
        Log.d(TAG, "WebSocket服务已停止");
    }

    /**
     * 订阅设备消息
     * @param deviceId 设备ID
     */
    public void subscribeDevice(String deviceId) {
        if (webSocketService != null) {
            webSocketService.subscribe(deviceId);
        }
    }

    /**
     * 取消订阅设备消息
     * @param deviceId 设备ID
     */
    public void unsubscribeDevice(String deviceId) {
        if (webSocketService != null) {
            webSocketService.unsubscribe(deviceId);
        }
    }

    /**
     * 构建Cookie字符串
     * @return Cookie字符串
     */
    private String buildCookieString() {
        if (sessionManager == null) return "";
        Set<String> cookies = sessionManager.getCookies();
        if (cookies == null || cookies.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String cookie : cookies) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(cookie);
        }
        return sb.toString();
    }

    /**
     * 收到报警事件回调
     * @param alert 报警数据
     */
    @Override
    public void onAlarmReceived(Alert alert) {
        Log.d(TAG, "收到报警: " + alert.getAlertType());
        alarmEvents.postValue(alert);
    }

    /**
     * 收到位置更新回调
     * @param location 设备位置数据
     */
    @Override
    public void onLocationUpdate(DeviceLocation location) {
        Log.d(TAG, "收到位置更新: " + location.getImei());
        locationEvents.postValue(location);
    }

    /**
     * 收到设备状态更新回调
     * @param status 设备状态数据
     */
    @Override
    public void onDeviceStatusUpdate(DeviceStatus status) {
        Log.d(TAG, "收到设备状态更新: " + status.getImei());
        statusEvents.postValue(status);
    }

    /**
     * 连接状态变化回调
     * @param connected 是否已连接
     */
    @Override
    public void onConnectionChanged(boolean connected) {
        Log.d(TAG, "连接状态变化: " + connected);
        connectionState.postValue(connected);
    }

    public LiveData<Boolean> getConnectionState() { return connectionState; }
    public LiveData<Alert> getAlarmEvents() { return alarmEvents; }
    public LiveData<DeviceLocation> getLocationEvents() { return locationEvents; }
    public LiveData<DeviceStatus> getStatusEvents() { return statusEvents; }

    /**
     * 获取当前连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        return webSocketService != null && webSocketService.isConnected();
    }
}
