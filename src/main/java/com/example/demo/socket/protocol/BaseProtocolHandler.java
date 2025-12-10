package com.example.demo.socket.protocol;

import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * BaseProtocolHandler - 基础协议处理器实现，提供通用功能
 */
public abstract class BaseProtocolHandler implements ProtocolHandler {
    
    protected static final String HEADER = "IW";
    protected static final String END_MARKER = "#";
    
    @Override
    public BraceletPacket parse(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        
        // 基本格式验证
        if (!raw.startsWith(HEADER) || !raw.endsWith(END_MARKER)) {
            return null;
        }
        if (raw.length() < 6) {
            return null; // 最小长度
        }
        
        try {
            return doParse(raw);
        } catch (Exception e) {
            // 解析失败，返回null
            return null;
        }
    }
    
    /**
     * 具体协议解析实现，由子类实现
     * 
     * @param raw 原始报文
     * @return 解析后的BraceletPacket对象
     * @throws Exception 解析异常
     */
    protected abstract BraceletPacket doParse(String raw) throws Exception;
    
    /**
     * 提取payload部分
     * 
     * @param raw 原始报文
     * @return payload部分
     */
    protected String extractPayload(String raw) {
        int payloadStart = 6; // 协议号之后
        return raw.substring(payloadStart, raw.length() - 1); // 去掉结束符
    }
    
    /**
     * 创建基础BraceletPacket对象
     * 
     * @param raw 原始报文
     * @param protocol 协议号
     * @return BraceletPacket对象
     */
    protected BraceletPacket createBasePacket(String raw, String protocol) {
        BraceletPacket packet = new BraceletPacket() {};
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        packet.setParams(new HashMap<>());
        return packet;
    }
    
    /**
     * 计算校验和
     * 
     * @param data 需要计算校验和的数据
     * @return 校验和值
     */
    protected int calculateChecksum(String data) {
        if (data == null) return 0;
        int checksum = 0;
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            checksum ^= b;
        }
        return checksum & 0xFF;
    }
    
    /**
     * 验证校验和
     * 
     * @param data 数据部分
     * @param checksumStr 校验和字符串
     * @return 校验和是否有效
     */
    protected boolean validateChecksum(String data, String checksumStr) {
        try {
            int expectedChecksum = Integer.parseInt(checksumStr, 16);
            int actualChecksum = calculateChecksum(data);
            return expectedChecksum == actualChecksum;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 从报文中提取校验和
     * 
     * @param raw 原始报文
     * @return 校验和字符串，若不存在则返回null
     */
    protected String extractChecksum(String raw) {
        // 检查报文是否包含校验和字段
        // 假设校验和字段位于payload的最后，格式为&CS=XX
        int csIndex = raw.indexOf("&CS=");
        if (csIndex != -1) {
            int endIndex = raw.indexOf('#', csIndex);
            if (endIndex != -1) {
                return raw.substring(csIndex + 4, endIndex);
            }
        }
        return null;
    }
    
    /**
     * 从报文中提取不包含校验和的数据部分
     * 
     * @param raw 原始报文
     * @return 不包含校验和的数据部分
     */
    protected String extractDataWithoutChecksum(String raw) {
        // 检查报文是否包含校验和字段
        int csIndex = raw.indexOf("&CS=");
        if (csIndex != -1) {
            return raw.substring(0, csIndex);
        }
        return raw;
    }
}