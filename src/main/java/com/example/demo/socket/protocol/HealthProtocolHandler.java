package com.example.demo.socket.protocol;

/**
 * HealthProtocolHandler - 处理AP02健康包协议
 */
public class HealthProtocolHandler extends BaseProtocolHandler {
    
    private static final String PROTOCOL = "AP02";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        HealthPacket packet = new HealthPacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        
        // 解析健康数据
        // 简单实现：按逗号拆分payload，具体格式根据协议文档调整
        String[] parts = payload.split(",");
        if (parts.length >= 1) {
            packet.setTimestamp(parts[0]);
            packet.getParams().put("timestamp", parts[0]);
        }
        if (parts.length >= 2) {
            packet.setHealthType(parts[1]);
            packet.getParams().put("healthType", parts[1]);
        }
        if (parts.length >= 3) {
            packet.setHealthValue(parts[2]);
            packet.getParams().put("healthValue", parts[2]);
        }
        if (parts.length >= 4) {
            packet.setUnit(parts[3]);
            packet.getParams().put("unit", parts[3]);
        }
        
        // 保存原始payload和协议信息
        packet.getParams().put("payload", payload);
        packet.getParams().put("protocol", protocol);
        
        return packet;
    }
}