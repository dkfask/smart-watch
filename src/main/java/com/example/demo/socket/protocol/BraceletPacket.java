package com.example.demo.socket.protocol;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * BraceletPacket - 手环上传包的通用数据模型（基类）
 *
 * 说明：
 * - 所有具体协议类型（AP00, AP01, AP03 等）都继承自此类。
 * - 本类保留原始报文、协议号和通用字段，子类提供各协议特有字段。
 */
public abstract class BraceletPacket {

    /** 原始接收到的明文报文（包含头尾），例如：IWAP00...# */
    protected String raw;

    /** 包头标识，一般为 IW */
    protected String header;

    /** 协议号，例如 AP00, AP01, AP03 等 */
    protected String protocol;

    /** 通用的 IMEI（如果报文包含） */
    protected String imei;

    /** 解析时记录的接收时间 */
    protected Date receiveTime = new Date();

    /** 可选的额外参数键值（解析器可填充） */
    protected Map<String, String> params = new HashMap<>();
    
    /** 协议版本号 */
    protected String version = "1.0"; // 默认版本号

    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }

    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public Date getReceiveTime() { return receiveTime; }
    public void setReceiveTime(Date receiveTime) { this.receiveTime = receiveTime; }

    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

}

