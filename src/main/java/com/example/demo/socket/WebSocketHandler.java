package com.example.demo.socket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket处理器，处理实时数据推送
 */
public class WebSocketHandler extends TextWebSocketHandler {

    // 在线连接数
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    
    // 存储所有活跃的WebSocket会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

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
        
        System.out.println("WebSocket连接建立: " + sessionId + ", 当前连接数: " + currentCount);
        
        // 发送连接成功消息
        sendMessage(session, createMessage("connection", "success", "连接成功"));
    }

    /**
     * 接收消息时调用
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到WebSocket消息: " + payload);
        
        // 可以根据需要处理客户端发送的消息
        // 例如：订阅特定设备的实时数据
        
        // 回复消息
        sendMessage(session, createMessage("message", "received", "消息已收到: " + payload));
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
        
        System.out.println("WebSocket连接关闭: " + sessionId + ", 当前连接数: " + currentCount);
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
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.println("发送WebSocket消息失败: " + e.getMessage());
            }
        }
    }

    /**
     * 创建消息格式
     */
    private static String createMessage(String type, String action, Object data) {
        return String.format("{\"type\":\"%s\",\"action\":\"%s\",\"data\":%s}",
                type, action, data instanceof String ? (String) data : toJson(data));
    }

    /**
     * 简单的对象转JSON方法（实际项目中建议使用Jackson或Gson）
     */
    private static String toJson(Object data) {
        if (data == null) {
            return "null";
        }
        return data.toString();
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
}