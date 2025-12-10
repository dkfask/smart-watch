package com.example.demo.socket.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * ProtocolHandlerFactory - 协议处理器工厂，用于根据协议类型和版本创建相应的处理器实例
 */
public class ProtocolHandlerFactory {
    
    // 协议处理器映射，key 为协议类型，value 为版本到处理器的映射
    private static final Map<String, Map<String, ProtocolHandler>> handlers = new HashMap<>();
    
    // 静态初始化，注册所有协议处理器
    static {
        registerHandler(new LoginProtocolHandler());
        registerHandler(new LocationProtocolHandler());
        registerHandler(new HeartbeatProtocolHandler());
        registerHandler(new HealthProtocolHandler());
        registerHandler(new LowBatteryProtocolHandler());
        registerHandler(new AlarmProtocolHandler());
        registerHandler(new ImageProtocolHandler());
        // 注册默认处理器，用于处理未知协议
        registerHandler(new DefaultProtocolHandler());
    }
    
    /**
     * 注册协议处理器（默认版本 1.0）
     * 
     * @param handler 协议处理器实例
     */
    public static void registerHandler(ProtocolHandler handler) {
        registerHandler(handler, "1.0");
    }
    
    /**
     * 注册协议处理器（指定版本）
     * 
     * @param handler 协议处理器实例
     * @param version 协议版本
     */
    public static void registerHandler(ProtocolHandler handler, String version) {
        if (handler != null && version != null) {
            String protocol = handler.getSupportedProtocol();
            handlers.computeIfAbsent(protocol, k -> new HashMap<>()).put(version, handler);
        }
    }
    
    /**
     * 根据协议类型和版本获取协议处理器
     * 
     * @param protocol 协议类型
     * @param version 协议版本，若为 null 则使用默认版本 1.0
     * @return 协议处理器实例，若找不到则返回默认处理器
     */
    public static ProtocolHandler getHandler(String protocol, String version) {
        if (protocol == null) {
            return handlers.get(DefaultProtocolHandler.DEFAULT_PROTOCOL).get("1.0");
        }
        
        Map<String, ProtocolHandler> versionMap = handlers.get(protocol);
        if (versionMap == null) {
            // 返回默认处理器
            return handlers.get(DefaultProtocolHandler.DEFAULT_PROTOCOL).get("1.0");
        }
        
        // 如果指定版本不存在，使用默认版本 1.0
        ProtocolHandler handler = versionMap.get(version);
        if (handler == null) {
            handler = versionMap.get("1.0");
        }
        
        // 如果默认版本也不存在，返回默认处理器
        if (handler == null) {
            handler = handlers.get(DefaultProtocolHandler.DEFAULT_PROTOCOL).get("1.0");
        }
        
        return handler;
    }
    
    /**
     * 根据协议类型获取协议处理器（默认版本 1.0）
     * 
     * @param protocol 协议类型
     * @return 协议处理器实例，若找不到则返回默认处理器
     */
    public static ProtocolHandler getHandler(String protocol) {
        return getHandler(protocol, "1.0");
    }
    
    /**
     * 默认协议处理器，用于处理未知协议类型
     */
    private static class DefaultProtocolHandler extends BaseProtocolHandler {
        
        private static final String DEFAULT_PROTOCOL = "DEFAULT";
        
        @Override
        public String getSupportedProtocol() {
            return DEFAULT_PROTOCOL;
        }
        
        @Override
        protected BraceletPacket doParse(String raw) throws Exception {
            String protocol = raw.substring(2, 6);
            String payload = extractPayload(raw);
            
            BraceletPacket packet = new BraceletPacket() {};
            packet.setRaw(raw);
            packet.setHeader(HEADER);
            packet.setProtocol(protocol);
            packet.getParams().put("payload", payload);
            
            return packet;
        }
    }
}