package com.example.demo.socket.protocol;

/**
 * LoginProtocolHandler - 处理AP00登录协议
 */
public class LoginProtocolHandler extends BaseProtocolHandler {
    
    private static final String PROTOCOL = "AP00";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        LoginPacket packet = new LoginPacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        
        // 解析payload的三种可能格式
        String[] parts = payload.split(",");
        if (parts.length >= 1) {
            String imeiPart = parts[0].trim();
            // 移除IMEI后面的额外参数（如&V=2.0）
            int ampIndex = imeiPart.indexOf('&');
            String imei = ampIndex != -1 ? imeiPart.substring(0, ampIndex) : imeiPart;
            packet.setImei(imei);
            packet.getParams().put("imei", imei);
        }
        
        if (parts.length >= 2) {
            String second = parts[1];
            if (second.contains("|")) {
                // 格式：IMEI,MCC|MNC|APN
                String[] net = second.split("\\|", 3);
                if (net.length >= 1) {
                    packet.setMcc(net[0]);
                    packet.getParams().put("mcc", net[0]);
                }
                if (net.length >= 2) {
                    packet.setMnc(net[1]);
                    packet.getParams().put("mnc", net[1]);
                }
                if (net.length >= 3) {
                    packet.setApn(net[2]);
                    packet.getParams().put("apn", net[2]);
                }
            } else if (parts.length >= 3) {
                // 格式：IMEI,ICCID,IMSI
                packet.setIccid(parts[1].trim());
                packet.setImsi(parts[2].trim());
                packet.getParams().put("iccid", parts[1].trim());
                packet.getParams().put("imsi", parts[2].trim());
            }
        }
        
        packet.getParams().put("payload", payload);
        return packet;
    }
}