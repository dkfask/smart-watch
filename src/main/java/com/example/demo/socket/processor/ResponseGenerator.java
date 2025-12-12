package com.example.demo.socket.processor;

import com.example.demo.socket.protocol.BraceletPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

/**
 * 响应生成器，负责生成设备响应
 */
public class ResponseGenerator {
    private static final Logger log = LoggerFactory.getLogger(ResponseGenerator.class);
    private static final String HEADER = "IW";
    private static final String END_MARKER = "#";
    private final String responseKey;
    private final boolean appendNewlineAfterResponse;
    private final LogProcessor logProcessor;

    public ResponseGenerator(String responseKey, boolean appendNewlineAfterResponse, LogProcessor logProcessor) {
        this.responseKey = responseKey;
        this.appendNewlineAfterResponse = appendNewlineAfterResponse;
        this.logProcessor = logProcessor;
    }

    /**
     * 创建回复给手环的数据包
     */
    public String createResponse(BraceletPacket packet) {
        String responseProtocol = "B" + packet.getProtocol().substring(1);

        if ("AP00".equals(packet.getProtocol())) {
            // 按协议要求：返回 IWBP00,20150101125223,8,Asia/Shanghai#
            // 其中时间为 UTC 0 时区时间（yyyyMMddHHmmss），第二项为服务器当前时区小时偏移
            java.time.format.DateTimeFormatter fmtUtc = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneOffset.UTC);
            String utc = fmtUtc.format(java.time.Instant.now());

            int tzHours = ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()).getTotalSeconds() / 3600;
            String zoneId = ZoneId.systemDefault().getId();
            if(responseKey != null && !responseKey.isEmpty()) {
                zoneId += "," + responseKey;
            }
            return HEADER + responseProtocol + "," + utc + "," + tzHours + "," + zoneId + END_MARKER;
        }

        // 其它包统一返回带状态或空的 BPxx（简单实现）
        return switch (packet.getProtocol()) {
            case "AP01" -> "IWBP01#";
            case "AP03" -> HEADER + "BP03" + END_MARKER;
            case "AP04" -> HEADER + "BP04" + END_MARKER;
            case "AP10" -> HEADER + "BP10" + END_MARKER;
            case "AP16" -> HEADER + "BP16" + END_MARKER;
            case "AP42" -> HEADER + "BP42" + END_MARKER;
            case "APBL" -> HEADER + "BPBL" + END_MARKER;
            case "APJK" -> HEADER + "BPJK" + END_MARKER;
            case "APTP" -> HEADER + "BPTP" + END_MARKER;
            case "APVR" -> HEADER + "BPVR" + END_MARKER;
            case "APWR" -> HEADER + "BPWR" + END_MARKER;
            default -> HEADER + responseProtocol + END_MARKER;
        };
    }

    /**
     * 将响应写回设备
     */
    public void writeFrame(BufferedOutputStream out, String s, Socket clientSocket, String clientInfo, String imei) throws IOException {
        if (out == null || s == null) return;
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(bytes);
        if (appendNewlineAfterResponse) {
            out.write('\r');
            out.write('\n');
        }
        out.flush();
        
        // 记录服务器回复信息
        log.debug("📤 发送回复: {} (newlineAppended={})");
        
        // 保存响应日志
        if (logProcessor != null) {
            logProcessor.saveResponseLog(s, imei, clientInfo);
        }
    }
}
