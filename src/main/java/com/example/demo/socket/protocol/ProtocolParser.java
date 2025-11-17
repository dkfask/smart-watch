package com.example.demo.socket.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * ProtocolParser - 将原始手环报文解析为具体的 BraceletPacket 子类
 *
 * 解析策略（保守方式，尽量不抛异常，仅进行必要解析）：
 * - 校验包头 IW 与结束符 #
 * - 提取协议号（位置 2-6）例如 AP00
 * - 对 AP00（登录）做专门解析，将 payload 放到 params.payload
 * - 对 AP01（位置）尝试解析日期/有效性/经纬度等字段，放入 params
 * - 对 AP03（心跳）将 payload 放入 params 并尝试解析常见子字段
 * - 其他协议会被当作通用包，payload 放入 params
 */
public class ProtocolParser {
    private static final String HEADER = "IW";
    private static final String END_MARKER = "#";

    public static BraceletPacket parse(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (!raw.startsWith(HEADER) || !raw.endsWith(END_MARKER)) return null;
        if (raw.length() < 6) return null; // 最小长度

        try {
            String protocol = raw.substring(2, 6);
            // payloadStartIndex: 协议号之后（不再自动尝试识别流水号）
            int payloadStart = 6;

            String payload = raw.substring(payloadStart, raw.length() - 1); // 去掉结束符

            Map<String, String> params = new HashMap<>();
            params.put("payload", payload);

            // 根据具体协议构建不同类型的包
            switch (protocol) {
                case "AP00": {
                    LoginPacket p = new LoginPacket();
                    p.setRaw(raw);
                    p.setHeader(HEADER);
                    p.setProtocol(protocol);
                    p.setReceiveTime(new java.util.Date());
                    p.setParams(params);

                    // 解析 payload 的三种可能格式
                    String[] parts = payload.split(",");
                    if (parts.length >= 1) p.setImei(parts[0].trim());
                    if (parts.length >= 2 && parts[1].contains("|")) {
                        String[] net = parts[1].split("\\|", 3);
                        if (net.length > 0) p.setMcc(net[0]);
                        if (net.length > 1) p.setMnc(net[1]);
                        if (net.length > 2) p.setApn(net[2]);
                    } else if (parts.length >= 3) {
                        p.setIccid(parts[1].trim());
                        p.setImsi(parts[2].trim());
                    }
                    // 保留原始 payload
                    p.getParams().putAll(params);
                    return p;
                }
                case "AP01": {
                    LocationPacket p = new LocationPacket();
                    p.setRaw(raw);
                    p.setHeader(HEADER);
                    p.setProtocol(protocol);
                    p.setReceiveTime(new java.util.Date());
                    p.setParams(params);

                    // AP01 示例一般第一个部分为 GPS 主段（直到第一个逗号），后续为基站/wifi
                    String gpsPart = payload;
                    String restPart = "";
                    int idx = payload.indexOf(',');
                    if (idx >= 0) {
                        gpsPart = payload.substring(0, idx);
                        restPart = payload.substring(idx + 1);
                    }
                    p.setExtra(restPart);
                    p.getParams().put("rawGpsPart", gpsPart);
                    p.getParams().put("rawExtraPart", restPart);

                    // gpsPart 例如: 080524A2232.9806N11404.9355E000.1061830323.87
                    if (gpsPart.length() >= 7) {
                        // date (6 chars)
                        String date = gpsPart.substring(0, 6);
                        p.setDate(date);
                        p.getParams().put("date", date);
                        int pos = 6;
                        if (pos < gpsPart.length()) {
                            String valid = gpsPart.substring(pos, pos + 1);
                            p.setValid(valid);
                            p.getParams().put("valid", valid);
                            pos += 1;

                            // 接下来尝试解析纬度 ddmm.mmmmN
                            int latEnd = indexOfAny(gpsPart, pos, new char[]{'N','S'});
                            if (latEnd > pos) {
                                String latRaw = gpsPart.substring(pos, latEnd + 1); // 包含 N/S
                                p.setLatRaw(latRaw);
                                p.getParams().put("latRaw", latRaw);
                                Double lat = convertNMEAToDecimal(latRaw);
                                if (lat != null) { p.setLat(lat); p.getParams().put("lat", String.valueOf(lat)); }
                                pos = latEnd + 1;
                            }

                            int lngEnd = indexOfAny(gpsPart, pos, new char[]{'E','W'});
                            if (lngEnd > pos) {
                                String lngRaw = gpsPart.substring(pos, lngEnd + 1);
                                p.setLngRaw(lngRaw);
                                p.getParams().put("lngRaw", lngRaw);
                                Double lng = convertNMEAToDecimal(lngRaw);
                                if (lng != null) { p.setLng(lng); p.getParams().put("lng", String.valueOf(lng)); }
                                pos = lngEnd + 1;
                            }

                            // 速度/时间/方向等剩余部分
                            if (pos < gpsPart.length()) {
                                String tail = gpsPart.substring(pos);
                                p.setSpeed(tail);
                                p.getParams().put("gpsTail", tail);
                            }
                        }
                    }

                    return p;
                }
                case "AP03": {
                    HeartbeatPacket p = new HeartbeatPacket();
                    p.setRaw(raw);
                    p.setHeader(HEADER);
                    p.setProtocol(protocol);
                    p.setReceiveTime(new java.util.Date());
                    p.setParams(params);
                    p.setRawPayload(payload);

                    // 进一步把 payload 按逗号拆分
                    String[] parts = payload.split(",");
                    if (parts.length >= 1) p.getParams().put("statusBlock", parts[0]);
                    if (parts.length >= 2) p.getParams().put("counter", parts[1]);
                    if (parts.length >= 3) p.getParams().put("rollCount", parts[2]);
                    if (parts.length >= 4) p.getParams().put("workMode", parts[3]);
                    if (parts.length >= 5) p.getParams().put("interval", parts[4]);

                    return p;
                }
                // 其他协议 -> 通用包
                default: {
                    BraceletPacket p = new BraceletPacket() {};
                    p.setRaw(raw);
                    p.setHeader(HEADER);
                    p.setProtocol(protocol);
                    p.setReceiveTime(new java.util.Date());
                    p.setParams(params);
                    return p;
                }
            }
        } catch (Exception e) {
            // 解析失败，返回 null
            return null;
        }
    }

    private static int indexOfAny(String s, int from, char[] targets) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            for (char t : targets) if (c == t) return i;
        }
        return -1;
    }

    /**
     * 把 NMEA 风格的 ddmm.mmmmN 或 dddmm.mmmmE 字符串转成十进制度
     */
    private static Double convertNMEAToDecimal(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.length() < 2) return null;
        char hemi = raw.charAt(raw.length() - 1);
        String body = raw.substring(0, raw.length() - 1);
        int dot = body.indexOf('.');
        if (dot <= 0) return null;
        try {
            int beforeDecimal = body.substring(0, dot).length();
            int degLen = beforeDecimal > 4 ? 3 : 2;
            String degStr = body.substring(0, degLen);
            String minStr = body.substring(degLen);
            double deg = Double.parseDouble(degStr);
            double minute = Double.parseDouble(minStr);
            double decimal = deg + (minute / 60.0);
            if (hemi == 'S' || hemi == 'W') decimal = -decimal;
            return decimal;
        } catch (Exception ex) {
            return null;
        }
    }
}
