package com.example.demo.socket.protocol;

/**
 * ImageProtocolHandler - 处理AP42图片包协议
 */
public class ImageProtocolHandler extends BaseProtocolHandler {
    
    private static final String PROTOCOL = "AP42";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        ImagePacket packet = new ImagePacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        
        // 解析图片包数据
        // 简单实现：按逗号拆分payload，具体格式根据协议文档调整
        String[] parts = payload.split(",");
        if (parts.length >= 1) {
            packet.setDeviceId(parts[0]);
            packet.getParams().put("deviceId", parts[0]);
        }
        if (parts.length >= 2) {
            packet.setTimestamp(parts[1]);
            packet.getParams().put("timestamp", parts[1]);
        }
        if (parts.length >= 3) {
            try {
                packet.setTotalFrames(Integer.parseInt(parts[2]));
                packet.getParams().put("totalFrames", parts[2]);
            } catch (NumberFormatException e) {
                packet.getParams().put("totalFrames", parts[2]);
            }
        }
        if (parts.length >= 4) {
            try {
                packet.setCurrentFrame(Integer.parseInt(parts[3]));
                packet.getParams().put("currentFrame", parts[3]);
            } catch (NumberFormatException e) {
                packet.getParams().put("currentFrame", parts[3]);
            }
        }
        if (parts.length >= 5) {
            packet.setImageData(parts[4]);
            packet.getParams().put("imageData", parts[4]);
        }
        if (parts.length >= 6) {
            packet.setImageFormat(parts[5]);
            packet.getParams().put("imageFormat", parts[5]);
        }
        if (parts.length >= 7) {
            packet.setImageSize(parts[6]);
            packet.getParams().put("imageSize", parts[6]);
        }
        
        // 判断是否为最后一帧
        if (parts.length >= 3 && parts.length >= 4) {
            try {
                int total = Integer.parseInt(parts[2]);
                int current = Integer.parseInt(parts[3]);
                packet.setLastFrame(current == total || current == total - 1);
                packet.getParams().put("isLastFrame", String.valueOf(packet.isLastFrame()));
            } catch (NumberFormatException e) {
                packet.setLastFrame(false);
                packet.getParams().put("isLastFrame", "false");
            }
        }
        
        // 保存原始payload和协议信息
        packet.getParams().put("payload", payload);
        packet.getParams().put("protocol", protocol);
        
        return packet;
    }
}