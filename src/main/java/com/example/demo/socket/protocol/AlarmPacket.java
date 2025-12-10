package com.example.demo.socket.protocol;

/**
 * AlarmPacket - 对应 AP10 报警上报包的具体数据模型
 * 
 * 本类解析并保存 AP10 上传的报警数据，格式通常为:
 * IWAP10+payload#
 * 
 * payload 格式示例:
 * 报警类型+设备ID+时间戳+位置信息+...
 */
public class AlarmPacket extends BraceletPacket {
    
    private String alarmType; // 报警类型
    private String deviceId; // 设备ID
    private String timestamp; // 时间戳
    private String latitude; // 纬度
    private String longitude; // 经度
    private String altitude; // 海拔
    private String speed; // 速度
    private String direction; // 方向
    private String batteryLevel; // 电池电量
    
    // Getters and setters
    public String getAlarmType() {
        return alarmType;
    }
    
    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getLatitude() {
        return latitude;
    }
    
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    
    public String getLongitude() {
        return longitude;
    }
    
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    
    public String getAltitude() {
        return altitude;
    }
    
    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }
    
    public String getSpeed() {
        return speed;
    }
    
    public void setSpeed(String speed) {
        this.speed = speed;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public String getBatteryLevel() {
        return batteryLevel;
    }
    
    public void setBatteryLevel(String batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
}