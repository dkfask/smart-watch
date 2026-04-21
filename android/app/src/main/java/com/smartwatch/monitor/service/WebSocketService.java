package com.smartwatch.monitor.service;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartwatch.monitor.model.Alert;
import com.smartwatch.monitor.model.DeviceLocation;
import com.smartwatch.monitor.model.DeviceStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket服务类，负责与后端建立WebSocket连接
 * 支持设备订阅、自动重连、消息解析等功能
 */
public class WebSocketService extends WebSocketListener {

    private static final String TAG = "WebSocketService";

    /** 最大重连间隔（毫秒） */
    private static final int MAX_RECONNECT_INTERVAL = 30000;

    /** 初始重连间隔（毫秒） */
    private static final int INITIAL_RECONNECT_INTERVAL = 1000;

    private WebSocket webSocket;
    private OkHttpClient okHttpClient;
    private Gson gson;
    private String wsUrl;
    private String sessionCookie;
    private boolean isConnected = false;
    private boolean shouldReconnect = true;
    private int reconnectInterval = INITIAL_RECONNECT_INTERVAL;
    private final Set<String> subscribedDevices = new HashSet<>();
    private WebSocketCallback callback;
    private Thread reconnectThread;

    /**
     * WebSocket回调接口，用于通知外部事件
     */
    public interface WebSocketCallback {
        /**
         * 收到报警事件
         * @param alert 报警数据
         */
        void onAlarmReceived(Alert alert);

        /**
         * 收到位置更新
         * @param location 设备位置数据
         */
        void onLocationUpdate(DeviceLocation location);

        /**
         * 收到设备状态更新
         * @param status 设备状态数据
         */
        void onDeviceStatusUpdate(DeviceStatus status);

        /**
         * 连接状态变化
         * @param connected 是否已连接
         */
        void onConnectionChanged(boolean connected);
    }

    /**
     * 构造函数，初始化WebSocket服务
     * @param wsUrl WebSocket服务器地址
     * @param sessionCookie 会话Cookie
     */
    public WebSocketService(String wsUrl, String sessionCookie) {
        this.wsUrl = wsUrl;
        this.sessionCookie = sessionCookie;
        this.gson = new Gson();
        this.okHttpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 设置回调监听器
     * @param callback 回调接口实例
     */
    public void setCallback(WebSocketCallback callback) {
        this.callback = callback;
    }

    /**
     * 建立WebSocket连接
     */
    public void connect() {
        if (isConnected) return;

        shouldReconnect = true;
        Request.Builder requestBuilder = new Request.Builder()
                .url(wsUrl);

        if (sessionCookie != null && !sessionCookie.isEmpty()) {
            requestBuilder.addHeader("Cookie", sessionCookie);
        }

        Request request = requestBuilder.build();
        webSocket = okHttpClient.newWebSocket(request, this);
        Log.d(TAG, "正在连接WebSocket: " + wsUrl);
    }

    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        shouldReconnect = false;
        isConnected = false;
        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }
        if (webSocket != null) {
            webSocket.close(1000, "用户主动断开");
            webSocket = null;
        }
        Log.d(TAG, "WebSocket已断开");
    }

    /**
     * 订阅设备消息
     * @param deviceId 设备ID
     */
    public void subscribe(String deviceId) {
        subscribedDevices.add(deviceId);
        if (isConnected && webSocket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("action", "subscribe");
            msg.addProperty("deviceId", deviceId);
            webSocket.send(gson.toJson(msg));
            Log.d(TAG, "已订阅设备: " + deviceId);
        }
    }

    /**
     * 取消订阅设备消息
     * @param deviceId 设备ID
     */
    public void unsubscribe(String deviceId) {
        subscribedDevices.remove(deviceId);
        if (isConnected && webSocket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("action", "unsubscribe");
            msg.addProperty("deviceId", deviceId);
            webSocket.send(gson.toJson(msg));
            Log.d(TAG, "已取消订阅设备: " + deviceId);
        }
    }

    /**
     * 发送心跳包
     */
    public void sendPing() {
        if (isConnected && webSocket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("action", "ping");
            webSocket.send(gson.toJson(msg));
            Log.d(TAG, "发送心跳包");
        }
    }

    /**
     * WebSocket连接打开回调
     * @param webSocket WebSocket实例
     * @param response HTTP响应
     */
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        isConnected = true;
        reconnectInterval = INITIAL_RECONNECT_INTERVAL;
        Log.d(TAG, "WebSocket连接已建立");

        for (String deviceId : subscribedDevices) {
            subscribe(deviceId);
        }

        if (callback != null) {
            callback.onConnectionChanged(true);
        }
    }

    /**
     * WebSocket收到消息回调
     * @param webSocket WebSocket实例
     * @param text 消息文本
     */
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d(TAG, "收到消息: " + text);
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                case "alarm":
                    Alert alert = gson.fromJson(json.get("data"), Alert.class);
                    if (callback != null) {
                        callback.onAlarmReceived(alert);
                    }
                    break;
                case "location":
                    DeviceLocation location = gson.fromJson(json.get("data"), DeviceLocation.class);
                    if (callback != null) {
                        callback.onLocationUpdate(location);
                    }
                    break;
                case "status":
                    DeviceStatus status = gson.fromJson(json.get("data"), DeviceStatus.class);
                    if (callback != null) {
                        callback.onDeviceStatusUpdate(status);
                    }
                    break;
                case "pong":
                    Log.d(TAG, "收到心跳响应");
                    break;
                default:
                    Log.d(TAG, "未知消息类型: " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "解析WebSocket消息失败: " + e.getMessage());
        }
    }

    /**
     * WebSocket连接关闭回调
     * @param webSocket WebSocket实例
     * @param code 关闭代码
     * @param reason 关闭原因
     */
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "WebSocket正在关闭: code=" + code + " reason=" + reason);
    }

    /**
     * WebSocket连接已关闭回调
     * @param webSocket WebSocket实例
     * @param code 关闭代码
     * @param reason 关闭原因
     */
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        isConnected = false;
        Log.d(TAG, "WebSocket已关闭: code=" + code + " reason=" + reason);
        if (callback != null) {
            callback.onConnectionChanged(false);
        }
        attemptReconnect();
    }

    /**
     * WebSocket连接失败回调
     * @param webSocket WebSocket实例
     * @param t 异常
     * @param response HTTP响应
     */
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        isConnected = false;
        Log.e(TAG, "WebSocket连接失败: " + t.getMessage());
        if (callback != null) {
            callback.onConnectionChanged(false);
        }
        attemptReconnect();
    }

    /**
     * 尝试自动重连，使用指数退避策略
     */
    private void attemptReconnect() {
        if (!shouldReconnect) return;

        reconnectThread = new Thread(() -> {
            try {
                Log.d(TAG, "将在 " + reconnectInterval + "ms 后重连...");
                Thread.sleep(reconnectInterval);
                if (shouldReconnect) {
                    reconnectInterval = Math.min(reconnectInterval * 2, MAX_RECONNECT_INTERVAL);
                    connect();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "重连线程被中断");
            }
        });
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    /**
     * 更新会话Cookie
     * @param cookie 新的会话Cookie
     */
    public void updateSessionCookie(String cookie) {
        this.sessionCookie = cookie;
    }

    /**
     * 获取连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }
}
