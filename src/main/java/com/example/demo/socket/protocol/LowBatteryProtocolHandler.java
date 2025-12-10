package com.example.demo.socket.protocol;

/**
 * LowBatteryProtocolHandler - 处理AP04低电报警协议
 */
public class LowBatteryProtocolHandler extends BaseProtocolHandler {
    
    private static final String PROTOCOL = "AP04";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        LowBatteryPacket packet = new LowBatteryPacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        
        // 解析低电报警数据
        // 简单实现：按逗号拆分payload，具体格式根据协议文档调整
        String[] parts = payload.split(",");
        if (parts.length >= 1) {
            packet.setDeviceId(parts[0]);
            packet.getParams().put("deviceId", parts[0]);
        }
        if (parts.length >= 2) {
            packet.setBatteryLevel(parts[1]);
            packet.getParams().put("batteryLevel", parts[1]);
        }
        if (parts.length >= 3) {
            packet.setTimestamp(parts[2]);
            packet.getParams().put("timestamp", parts[2]);
        }
        if (parts.length >= 4) {
            packet.setLocation(parts[3]);
            packet.getParams().put("location", parts[3]);
        }
        
        // 保存原始payload和协议信息
        packet.getParams().put("payload", payload);
        packet.getParams().put("protocol", protocol);
        
        return packet;
    }
}