package com.example.demo.socket.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LocationProtocolHandler - 处理AP01定位协议
 */
public class LocationProtocolHandler extends BaseProtocolHandler {
    
    private static final String PROTOCOL = "AP01";
    
    @Override
    public String getSupportedProtocol() {
        return PROTOCOL;
    }
    
    @Override
    protected BraceletPacket doParse(String raw) throws Exception {
        String protocol = raw.substring(2, 6);
        String payload = extractPayload(raw);
        
        LocationPacket packet = new LocationPacket();
        packet.setRaw(raw);
        packet.setHeader(HEADER);
        packet.setProtocol(protocol);
        
        // 按新协议格式解析：time(6) + valid(1) + lat(ddmm.mmmmN) + lng(dddmm.mmmmE) + speed + gpsTime(6) + course + params(14) + , + LBS(4 fields) + , + WIFI(groups)
        String gpsPart = payload;
        String restPart = "";
        int idx = payload.indexOf(',');
        if (idx >= 0) {
            gpsPart = payload.substring(0, idx);
            restPart = payload.substring(idx + 1);
        }
        packet.setExtra(restPart);
        packet.getParams().put("rawGpsPart", gpsPart);
        packet.getParams().put("rawExtraPart", restPart);
        
        String valid = null;
        String latRaw = null;
        String lngRaw = null;
        
        if (gpsPart.length() >= 7) {
            // 时间 6 位
            String date = gpsPart.substring(0, 6);
            packet.setDate(date);
            packet.getParams().put("date", date);
            
            int pos = 6;
            // 直接进行后续解析，使用 Math.min 防止越界；有效性标志 1 位
            valid = gpsPart.substring(pos, Math.min(pos + 1, gpsPart.length()));
            packet.setValid(valid);
            packet.getParams().put("valid", valid);
            pos += 1;
            
            // 纬度 ddmm.mmmm + N/S
            int latEnd = indexOfAny(gpsPart, pos, new char[]{'N', 'S'});
            if (latEnd > pos) {
                latRaw = gpsPart.substring(pos, latEnd + 1);
                packet.setLatRaw(latRaw);
                packet.getParams().put("latRaw", latRaw);
                packet.getParams().put("lat_raw", latRaw); // 同时保存为lat_raw，确保与数据库字段兼容
                Double lat = convertNMEAToDecimal(latRaw);
                if (lat != null) {
                    packet.setLat(lat);
                    packet.getParams().put("lat", String.valueOf(lat));
                }
                pos = latEnd + 1;
            }
            
            // 经度 dddmm.mmmm + E/W
            int lngEnd = indexOfAny(gpsPart, pos, new char[]{'E', 'W'});
            if (lngEnd > pos) {
                lngRaw = gpsPart.substring(pos, lngEnd + 1);
                packet.setLngRaw(lngRaw);
                packet.getParams().put("lngRaw", lngRaw);
                packet.getParams().put("lng_raw", lngRaw); // 同时保存为lng_raw，确保与数据库字段兼容
                Double lng = convertNMEAToDecimal(lngRaw);
                if (lng != null) {
                    packet.setLng(lng);
                    packet.getParams().put("lon", String.valueOf(lng));
                    packet.getParams().put("lng", String.valueOf(lng)); // 同时保存为lng，确保兼容性
                }
                pos = lngEnd + 1;
            }
            
            // 剩余部分通常为 speed + gpsTime(6) + course + paramsDigits
            if (pos < gpsPart.length()) {
                String tail = gpsPart.substring(pos);
                // 先提取 speed（包含小数）和 6 位 gpsTime，剩余部分作为 course+params
                Pattern pat = Pattern.compile("([0-9]{3}\\.[0-9]+)");
                Matcher m = pat.matcher(tail);
                if (m.find()) {
                    String speed = m.group(1);
                    String gpsTime = "";
                    String course = "";
                    String paramsStr = "";
                    
                    int mEnd = m.end();
                    if (tail.length() > mEnd) {
                        String rest = tail.substring(mEnd);
                        // gpsTime 6位
                        if (rest.length() >= 6) {
                            gpsTime = rest.substring(0, 6);
                            rest = rest.substring(6);
                            // direction 可为浮点
                            Pattern m2 = Pattern.compile("([0-9]{1,3}\\.[0-9]+)");
                            Matcher m2Matcher = m2.matcher(rest);
                            if (m2Matcher.find()) {
                                course = m2Matcher.group(1);
                                rest = rest.substring(m2Matcher.end());
                            }
                            paramsStr = rest;
                        }
                    }
                    
                    packet.setSpeed(speed);
                    packet.setGpsTime(gpsTime);
                    packet.setDirection(course);
                    
                    packet.getParams().put("speed", speed);
                    packet.getParams().put("gps_time", gpsTime);
                    packet.getParams().put("direction", course);
                    
                    // 解析固定长度的参数段（尽量按协议：3+3+3+1+2+2 = 14）
                    if (!paramsStr.isEmpty() && paramsStr.length() >= 14) {
                        try {
                            String gsm = paramsStr.substring(0, 3);
                            String satellite = paramsStr.substring(3, 6);
                            String battery = paramsStr.substring(6, 9);
                            String reserved = paramsStr.substring(9, 10);
                            String arm = paramsStr.substring(10, 12);
                            String workMode = paramsStr.substring(12, 14);
                            packet.getParams().put("gsm", gsm);
                            packet.getParams().put("satellite", satellite);
                            packet.getParams().put("battery", battery);
                            packet.getParams().put("reserved", reserved);
                            packet.getParams().put("arm", arm);
                            packet.getParams().put("workMode", workMode);
                            packet.getParams().put("status_block", paramsStr); // 将整个paramsStr作为status_block保存
                        } catch (Exception ex) {
                            packet.getParams().put("paramsStr", paramsStr);
                            packet.getParams().put("status_block", paramsStr); // 异常时也保存status_block
                        }
                    } else if (!paramsStr.isEmpty()) {
                        packet.getParams().put("paramsStr", paramsStr);
                        packet.getParams().put("status_block", paramsStr); // 非标准长度时也保存status_block
                    }
                } else {
                    // 无法匹配初步结构，保留原始 tail
                    packet.setSpeed(tail);
                    packet.getParams().put("gpsTail", tail);
                    packet.getParams().put("status_block", tail); // 无法解析时保存tail为status_block
                }
            }
        }
        
        // 解析 LBS 与 Wi-Fi（restPart）
        // 如果 restPart 末尾包含 ",[lat@lon]"，先剥离出来单独保存为 wifiGeoLat/wifiGeoLon
        if (restPart != null && restPart.endsWith("]")) {
            int coordIdx = restPart.lastIndexOf(",[");
            if (coordIdx >= 0) {
                String coordPart = restPart.substring(coordIdx + 2, restPart.length() - 1);
                int at = coordPart.indexOf('@');
                if (at > 0) {
                    String wifiGeoLat = coordPart.substring(0, at);
                    String wifiGeoLon = coordPart.substring(at + 1);
                    packet.getParams().put("wifiGeoLat", wifiGeoLat);
                    packet.getParams().put("wifiGeoLon", wifiGeoLon);
                    // 从 restPart 中剥离掉末尾的 ",[lat@lon]"
                    restPart = restPart.substring(0, coordIdx);
                    packet.getParams().put("rawExtraPart", restPart); // 更新 rawExtraPart
                }
            }
        }
        
        if (!restPart.isEmpty()) {
            // 尝试拆成 4 个 LBS 字段 + 剩余 Wi-Fi 字段（restPart 结构：mcc,mnc,lac,cid,<wifi...>）
            String[] restTokens = restPart.split(",", 5);
            if (restTokens.length >= 4) {
                packet.getParams().put("mcc", restTokens[0].trim());
                packet.getParams().put("mnc", restTokens[1].trim());
                packet.getParams().put("lac", restTokens[2].trim());
                packet.getParams().put("cid", restTokens[3].trim());
            }
            if (restTokens.length >= 5) {
                String wifiRaw = restTokens[4].trim();
                packet.getParams().put("wifiRaw", wifiRaw);
                
                // Wi-Fi 可能有多组，用 & 分隔，每组内用 | 分隔：SSID|MAC|RSSI
                String[] groups = wifiRaw.split("&");
                int gi = 0;
                for (String g : groups) {
                    String gg = g.trim();
                    if (gg.isEmpty()) continue;
                    String[] parts = gg.split("\\|");
                    String ssid = parts.length > 0 ? parts[0].trim() : "";
                    String mac = parts.length > 1 ? parts[1].trim() : "";
                    String rssi = parts.length > 2 ? parts[2].trim() : "";
                    // 清理 SSID：去掉特殊字符（粗略处理：只保留可打印 ASCII，若为空则用索引代替）
                    String cleanSsid = ssid.replaceAll("[^ -~]", "").trim();
                    if (cleanSsid.isEmpty()) cleanSsid = "wifi" + gi;
                    packet.getParams().put("wifi." + gi + ".ssid", cleanSsid);
                    packet.getParams().put("wifi." + gi + ".mac", mac);
                    packet.getParams().put("wifi." + gi + ".rssi", rssi);
                    gi++;
                }
            }
        }
        
        // 回填：若 GPS 坐标缺失或为占位零值，则使用 wifiGeoLat/wifiGeoLon 作为最终坐标
        boolean gpsCoordIsPlaceholder = (latRaw == null || lngRaw == null
                || latRaw.startsWith("0000.0000") || lngRaw.startsWith("00000.0000"));
        boolean hasWifiGeo = packet.getParams().containsKey("wifiGeoLat") && packet.getParams().containsKey("wifiGeoLon");
        if (hasWifiGeo) {
            // 仅在 GPS 无效或缺失时回填，避免覆盖真实 GPS
            if ((packet.getLat() == null || packet.getLng() == null) || gpsCoordIsPlaceholder) {
                String wlat = packet.getParams().get("wifiGeoLat");
                String wlon = packet.getParams().get("wifiGeoLon");
                try {
                    if (wlat != null && !wlat.isEmpty()) {
                        Double dv = Double.parseDouble(wlat);
                        packet.setLat(dv);
                        // 若 params 中尚无 lat 键，补充（供后续统一访问）
                        if (!packet.getParams().containsKey("lat")) packet.getParams().put("lat", wlat);
                    }
                } catch (NumberFormatException ignore) {}
                try {
                    if (wlon != null && !wlon.isEmpty()) {
                        Double dv = Double.parseDouble(wlon);
                        packet.setLng(dv);
                        if (!packet.getParams().containsKey("lon") && !packet.getParams().containsKey("lng")) packet.getParams().put("lon", wlon);
                    }
                } catch (NumberFormatException ignore) {}
                packet.getParams().put("locationSource", "wifi");
            }
        }
        
        // 当 GPS 无效或经纬度为占位零值时，标记使用 LBS/Wi-Fi
        boolean gpsValid = "A".equalsIgnoreCase(valid)
                && latRaw != null && lngRaw != null
                && !latRaw.startsWith("0000.0000")
                && !lngRaw.startsWith("00000.0000");
        if (!gpsValid) {
            packet.getParams().put("useLbsOrWifi", "true");
            packet.getParams().put("locationSource", "lbs"); // 默认使用lbs作为非GPS定位源
        } else {
            packet.getParams().put("useGps", "true");
            packet.getParams().put("locationSource", "gps"); // GPS有效时明确标记为gps
        }
        
        // 保存原始GPS部分和额外部分到params，便于后续保存到数据库
        packet.getParams().put("gps_raw", gpsPart);
        packet.getParams().put("extra_raw", restPart);
        packet.getParams().put("payload", payload);
        packet.getParams().put("source", packet.getParams().get("locationSource")); // 将定位源保存到source字段
        return packet;
    }
    
    /**
     * 查找字符串中任意目标字符的位置
     */
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