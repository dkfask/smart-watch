package com.example.demo.socket.protocol;

/**
 * HeartbeatPacket - 对应 AP03 心跳包的简化模型
 *
 * 内容通常以逗号分隔，例如：AP03,06000908000102,5555,30#
 * 本类将主要保存 rawPayload 与 parts 供更高层解析使用。
 */
public class HeartbeatPacket extends BraceletPacket {
    private String rawPayload;

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
}

