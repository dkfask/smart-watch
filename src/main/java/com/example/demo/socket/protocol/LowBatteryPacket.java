package com.example.demo.socket.protocol;

/**
 * LowBatteryPacket - 对应 AP04 低电报警包的具体数据模型
 * 
 * 本类解析并保存 AP04 上传的低电报警数据，格式通常为:
 * IWAP04+payload#
 * 
 * payload 格式示例:
 * 设备ID+电池电量+时间戳+...
 */
public class LowBatteryPacket extends BraceletPacket {
    
    private String deviceId; // 设备ID
    private String batteryLevel; // 电池电量
    private String timestamp; // 时间戳
    private String location; // 位置信息
    
    // Getters and setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getBatteryLevel() {
        return batteryLevel;
    }
    
    public void setBatteryLevel(String batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
}