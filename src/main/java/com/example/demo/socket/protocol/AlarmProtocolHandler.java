package com.example.demo.socket.protocol;

/**
 * AlarmProtocolHandler - 处理AP10报警上报协议
 */
public class AlarmProtocolHandler extends BaseProtocolHandler {
    
    private static final String PROTOCOL = "AP10";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        AlarmPacket packet = new AlarmPacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        
        // 解析报警数据
        // 简单实现：按逗号拆分payload，具体格式根据协议文档调整
        String[] parts = payload.split(",");
        if (parts.length >= 1) {
            packet.setAlarmType(parts[0]);
            packet.getParams().put("alarmType", parts[0]);
        }
        if (parts.length >= 2) {
            packet.setDeviceId(parts[1]);
            packet.getParams().put("deviceId", parts[1]);
        }
        if (parts.length >= 3) {
            packet.setTimestamp(parts[2]);
            packet.getParams().put("timestamp", parts[2]);
        }
        if (parts.length >= 4) {
            packet.setLatitude(parts[3]);
            packet.getParams().put("latitude", parts[3]);
        }
        if (parts.length >= 5) {
            packet.setLongitude(parts[4]);
            packet.getParams().put("longitude", parts[4]);
        }
        if (parts.length >= 6) {
            packet.setAltitude(parts[5]);
            packet.getParams().put("altitude", parts[5]);
        }
        if (parts.length >= 7) {
            packet.setSpeed(parts[6]);
            packet.getParams().put("speed", parts[6]);
        }
        if (parts.length >= 8) {
            packet.setDirection(parts[7]);
            packet.getParams().put("direction", parts[7]);
        }
        if (parts.length >= 9) {
            packet.setBatteryLevel(parts[8]);
            packet.getParams().put("batteryLevel", parts[8]);
        }
        
        // 保存原始payload和协议信息
        packet.getParams().put("payload", payload);
        packet.getParams().put("protocol", protocol);
        
        return packet;
    }
}