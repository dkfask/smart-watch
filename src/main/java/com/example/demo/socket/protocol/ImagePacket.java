package com.example.demo.socket.protocol;

/**
 * ImagePacket - 对应 AP42 图片包的具体数据模型
 * 
 * 本类解析并保存 AP42 上传的图片数据，格式通常为:
 * IWAP42+payload#
 * 
 * payload 格式示例:
 * 设备ID+时间戳+总帧数+当前帧号+图片数据+...
 */
public class ImagePacket extends BraceletPacket {
    
    private String deviceId; // 设备ID
    private String timestamp; // 时间戳
    private int totalFrames; // 总帧数
    private int currentFrame; // 当前帧号
    private String imageData; // 图片数据（十六进制字符串）
    private String imageFormat; // 图片格式
    private String imageSize; // 图片大小
    private boolean isLastFrame; // 是否为最后一帧
    
    // Getters and setters
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
    
    public int getTotalFrames() {
        return totalFrames;
    }
    
    public void setTotalFrames(int totalFrames) {
        this.totalFrames = totalFrames;
    }
    
    public int getCurrentFrame() {
        return currentFrame;
    }
    
    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }
    
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    public String getImageFormat() {
        return imageFormat;
    }
    
    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }
    
    public String getImageSize() {
        return imageSize;
    }
    
    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
    
    public boolean isLastFrame() {
        return isLastFrame;
    }
    
    public void setLastFrame(boolean isLastFrame) {
        this.isLastFrame = isLastFrame;
    }
}