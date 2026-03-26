package com.example.demo.socket.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HeartbeatProtocolHandler - 处理AP03心跳协议
 */
public class HeartbeatProtocolHandler extends BaseProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatProtocolHandler.class);
    
    private static final String PROTOCOL = "AP03";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        HeartbeatPacket packet = new HeartbeatPacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        packet.setRawPayload(payload);
        
        // 进一步把 payload 按逗号拆分
        String[] parts = payload.split(",");
        if (parts.length >= 1) {
            String statusBlock = parts[0];
            packet.getParams().put("statusBlock", statusBlock);
            packet.getParams().put("status_block", statusBlock); // 同时保存为status_block，确保与数据库字段兼容
            
            // 解析电池电量：位于statusBlock的第6-9位（从0开始计数）
            if (statusBlock.length() >= 9) {
                try {
                    String battery = statusBlock.substring(6, 9);
                    packet.getParams().put("battery", battery);
                    packet.getParams().put("batteryLevel", battery);
                    packet.getParams().put("battery_level", battery); // 同时保存为battery_level，确保与数据库字段兼容
                } catch (Exception e) {
                    // 异常处理，不影响其他解析
                    log.warn("Failed to parse battery level from status block: {}", statusBlock, e);
                }
            }
        }
        if (parts.length >= 2) {
            packet.getParams().put("counter", parts[1]);
            packet.getParams().put("steps", parts[1]); // 同时保存为steps，确保兼容性
        }
        if (parts.length >= 3) {
            packet.getParams().put("rollCount", parts[2]);
            packet.getParams().put("roll_count", parts[2]); // 同时保存为roll_count，确保与数据库字段兼容
        }
        if (parts.length >= 4) {
            packet.getParams().put("workMode", parts[3]);
            packet.getParams().put("work_mode", parts[3]); // 同时保存为work_mode，确保与数据库字段兼容
        }
        if (parts.length >= 5) {
            packet.getParams().put("interval", parts[4]);
            packet.getParams().put("interval_seconds", parts[4]); // 同时保存为interval_seconds，确保与数据库字段兼容
        }
        
        // 保存原始payload和协议信息
        packet.getParams().put("payload", payload);
        packet.getParams().put("protocol", protocol);
        
        return packet;
    }
}