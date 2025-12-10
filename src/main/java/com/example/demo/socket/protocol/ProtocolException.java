package com.example.demo.socket.protocol;

/**
 * ProtocolException - 协议相关异常类，用于统一处理协议解析和处理过程中的异常
 */
public class ProtocolException extends RuntimeException {
    
    private String rawMessage;
    private String protocol;
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public ProtocolException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 异常原因
     */
    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param rawMessage 原始报文
     */
    public ProtocolException(String message, String rawMessage) {
        super(message);
        this.rawMessage = rawMessage;
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param rawMessage 原始报文
     * @param protocol 协议类型
     */
    public ProtocolException(String message, String rawMessage, String protocol) {
        super(message);
        this.rawMessage = rawMessage;
        this.protocol = protocol;
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param rawMessage 原始报文
     * @param cause 异常原因
     */
    public ProtocolException(String message, String rawMessage, Throwable cause) {
        super(message, cause);
        this.rawMessage = rawMessage;
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param rawMessage 原始报文
     * @param protocol 协议类型
     * @param cause 异常原因
     */
    public ProtocolException(String message, String rawMessage, String protocol, Throwable cause) {
        super(message, cause);
        this.rawMessage = rawMessage;
        this.protocol = protocol;
    }
    
    /**
     * 获取原始报文
     * 
     * @return 原始报文
     */
    public String getRawMessage() {
        return rawMessage;
    }
    
    /**
     * 设置原始报文
     * 
     * @param rawMessage 原始报文
     */
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }
    
    /**
     * 获取协议类型
     * 
     * @return 协议类型
     */
    public String getProtocol() {
        return protocol;
    }
    
    /**
     * 设置协议类型
     * 
     * @param protocol 协议类型
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}