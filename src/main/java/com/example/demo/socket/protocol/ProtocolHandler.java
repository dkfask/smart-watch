package com.example.demo.socket.protocol;

/**
 * ProtocolHandler - 协议处理器接口，定义协议处理方法
 * 
 * 说明：
 * - 每种协议类型都应实现此接口
 * - 负责将原始报文解析为BraceletPacket对象
 */
public interface ProtocolHandler {
    
    /**
     * 解析原始报文为BraceletPacket对象
     * 
     * @param raw 原始报文
     * @return 解析后的BraceletPacket对象，解析失败返回null
     */
    BraceletPacket parse(String raw);
    
    /**
     * 获取支持的协议类型
     * 
     * @return 协议类型，如"AP00", "AP01"等
     */
    String getSupportedProtocol();
}