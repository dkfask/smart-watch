package com.example.demo.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket处理器，处理实时数据推送
 */
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    // 在线连接数
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    
    // 存储所有活跃的WebSocket会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 设备ID到会话ID的映射，用于快速查找设备相关的会话
    private static final Map<String, Set<String>> deviceSessionMap = new ConcurrentHashMap<>();
    
    // 消息发送线程池，用于异步发送消息
    private static final ExecutorService messageExecutor = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r, "websocket-message-sender-");
        t.setDaemon(true);
        return t;
    });
    
    // 消息队列，用于缓冲消息
    private static final BlockingQueue<MessageTask> messageQueue = new LinkedBlockingQueue<>(10000);
    
    // Jackson ObjectMapper，用于JSON序列化
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 静态初始化，启动消息处理线程
    static {
        for (int i = 0; i < 5; i++) {
            messageExecutor.submit(() -> {
                while (true) {
                    try {
                        MessageTask task = messageQueue.take();
                        task.execute();
                    } catch (Exception e) {
                        log.error("Error processing message task", e);
                    }
                }
            });
        }
    }
    
    // 消息任务类
    private static class MessageTask {
        private final WebSocketSession session;
        private final String message;
        
        public MessageTask(WebSocketSession session, String message) {
            this.session = session;
            this.message = message;
        }
        
        public void execute() {
            sendMessage(session, message);
        }
    }

    /**
     * 连接建立时调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 增加连接数
        int currentCount = connectionCount.incrementAndGet();
        
        // 存储会话
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        // 初始化设备订阅集合
        deviceSessionMap.put(sessionId, ConcurrentHashMap.newKeySet());
        
        log.info("WebSocket连接建立: {}, 当前连接数: {}", sessionId, currentCount);
        
        // 发送连接成功消息
        sendMessage(session, createMessage("connection", "success", "连接成功"));
    }

    /**
     * 接收消息时调用
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到WebSocket消息: {}", payload);
        
        try {
            // 解析客户端消息
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String action = (String) messageData.get("action");
            
            switch (action) {
                case "subscribe":
                    handleSubscribe(session, messageData);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(session, messageData);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    // 回复消息
                    sendMessage(session, createMessage("message", "received", "消息已收到: " + payload));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage());
            sendMessage(session, createMessage("error", "invalid_message", "消息格式无效"));
        }
    }
    
    /**
     * 处理设备订阅
     */
    private void handleSubscribe(WebSocketSession session, Map<String, Object> messageData) {
        String sessionId = session.getId();
        Set<String> subscribedDevices = deviceSessionMap.get(sessionId);
        
        if (subscribedDevices != null) {
            Object deviceIdObj = messageData.get("deviceId");
            if (deviceIdObj != null) {
                String deviceId = deviceIdObj.toString();
                subscribedDevices.add(deviceId);
                log.info("会话 {} 订阅设备: {}", sessionId, deviceId);
                sendMessage(session, createMessage("subscribe", "success", "订阅成功: " + deviceId));
            }
        }
    }
    
    /**
     * 处理设备取消订阅
     */
    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> messageData) {
        String sessionId = session.getId();
        Set<String> subscribedDevices = deviceSessionMap.get(sessionId);
        
        if (subscribedDevices != null) {
            Object deviceIdObj = messageData.get("deviceId");
            if (deviceIdObj != null) {
                String deviceId = deviceIdObj.toString();
                subscribedDevices.remove(deviceId);
                log.info("会话 {} 取消订阅设备: {}", sessionId, deviceId);
                sendMessage(session, createMessage("unsubscribe", "success", "取消订阅成功: " + deviceId));
            }
        }
    }
    
    /**
     * 处理ping消息
     */
    private void handlePing(WebSocketSession session) {
        sendMessage(session, createMessage("pong", "success", System.currentTimeMillis()));
    }

    /**
     * 连接关闭时调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 减少连接数
        int currentCount = connectionCount.decrementAndGet();
        
        // 移除会话
        String sessionId = session.getId();
        sessions.remove(sessionId);
        deviceSessionMap.remove(sessionId);
        
        log.info("WebSocket连接关闭: {}, 当前连接数: {}, 关闭状态: {}", sessionId, currentCount, status);
    }

    /**
     * 发送消息给所有客户端
     */
    public static void broadcast(String type, String action, Object data) {
        String message = createMessage(type, action, data);
        for (WebSocketSession session : sessions.values()) {
            sendMessage(session, message);
        }
    }
    
    /**
     * 发送消息给订阅了特定设备的客户端
     */
    public static void broadcastToDevice(String deviceId, String type, String action, Object data) {
        String message = createMessage(type, action, data);
        for (Map.Entry<String, Set<String>> entry : deviceSessionMap.entrySet()) {
            String sessionId = entry.getKey();
            Set<String> subscribedDevices = entry.getValue();
            if (subscribedDevices.contains(deviceId)) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * 发送消息给指定客户端
     */
    public static void sendToSession(String sessionId, String type, String action, Object data) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            String message = createMessage(type, action, data);
            sendMessage(session, message);
        }
    }

    /**
     * 发送消息
     */
    private static void sendMessage(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                // 将消息加入队列，异步发送
                boolean added = messageQueue.offer(new MessageTask(session, message), 1, TimeUnit.SECONDS);
                if (!added) {
                    log.warn("消息队列已满，丢弃消息: {}", message.length());
                }
            } catch (Exception e) {
                log.error("发送WebSocket消息失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建消息格式
     */
    private static String createMessage(String type, String action, Object data) {
        return String.format("{\"type\":\"%s\",\"action\":\"%s\",\"data\":%s,\"timestamp\":%d}",
                type, action, data instanceof String ? (String) data : toJson(data), System.currentTimeMillis());
    }

    /**
     * 使用Jackson进行JSON序列化
     */
    private static String toJson(Object data) {
        if (data == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            return data.toString();
        }
    }

    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return connectionCount.get();
    }

    /**
     * 获取在线会话数
     */
    public static int getSessionCount() {
        return sessions.size();
    }
    
    /**
     * 推送报警消息
     */
    public static void pushAlarm(Object alarm) {
        broadcast("alarm", "new", alarm);
    }
    
    /**
     * 推送报警状态更新
     */
    public static void pushAlarmUpdate(Object alarm) {
        broadcast("alarm", "update", alarm);
    }
    
    /**
     * 推送设备状态更新
     */
    public static void pushDeviceStatusUpdate(String deviceId, Object status) {
        // 推送设备状态更新给订阅了该设备的客户端
        broadcastToDevice(deviceId, "device_status_update", "update", status);
        // 同时广播给所有客户端
        broadcast("device_status_update", "update", status);
    }
    
    /**
     * 推送位置更新
     */
    public static void pushLocationUpdate(String deviceId, Object location) {
        // 推送位置更新给订阅了该设备的客户端
        broadcastToDevice(deviceId, "location_update", "update", location);
        // 同时广播给所有客户端
        broadcast("location_update", "update", location);
    }
    
    /**
     * 清理过期会话
     */
    public static void cleanupExpiredSessions() {
        int cleaned = 0;
        for (String sessionId : sessions.keySet()) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && !session.isOpen()) {
                sessions.remove(sessionId);
                deviceSessionMap.remove(sessionId);
                connectionCount.decrementAndGet();
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("清理了 {} 个过期会话", cleaned);
        }
    }
}