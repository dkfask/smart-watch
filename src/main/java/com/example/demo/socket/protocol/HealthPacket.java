package com.example.demo.socket.protocol;

/**
 * HealthPacket - 对应 AP02 健康包的具体数据模型
 * 
 * 本类解析并保存 AP02 上传的健康数据，格式通常为:
 * IWAP02+payload#
 * 
 * payload 格式示例:
 * 时间+类型+数值+单位+...
 */
public class HealthPacket extends BraceletPacket {
    
    private String healthType; // 健康数据类型
    private String healthValue; // 健康数据值
    private String unit; // 单位
    private String timestamp; // 时间戳
    
    // Getters and setters
    public String getHealthType() {
        return healthType;
    }
    
    public void setHealthType(String healthType) {
        this.healthType = healthType;
    }
    
    public String getHealthValue() {
        return healthValue;
    }
    
    public void setHealthValue(String healthValue) {
        this.healthValue = healthValue;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}