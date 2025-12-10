import java.util.*;
import java.util.regex.*;

public class ProtocolParserTest {
    public static void main(String[] args) {
        String sample = "IWAP01251124V0000.0000N00000.0000E000.0065617000.0005800009500008,460,04,32845,96788377,AP1|5c:02:14:b3:4d:56|-58&AP2|5e:02:14:a3:4d:56|-58&AP3|06:05:88:1e:73:3a|-68&AP4|06:05:88:1e:6d:ba|-72&AP5|c6:36:e2:80:d8:1f|-74,[30.886935@103.594551]#";
        // 去掉前后 IW ... #，只传入 payload 部分（与 ProtocolParser 相同的截取方式）
        String payload = sample.substring(6, sample.length()-1);
        Map<String,String> params = parseAP01(payload);
        System.out.println("解析结果:");
        for (Map.Entry<String,String> e : params.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue());
        }
    }

    public static Map<String,String> parseAP01(String payload) {
        Map<String,String> p = new LinkedHashMap<>();
        p.put("payload", payload);

        String gpsPart = payload;
        String restPart = "";
        int idx = payload.indexOf(',');
        if (idx >= 0) {
            gpsPart = payload.substring(0, idx);
            restPart = payload.substring(idx + 1);
        }
        // 如果 restPart 末尾包含 ,[lat@lon] 格式的坐标，把它剥离出来单独保存
        if (restPart != null && restPart.endsWith("]")) {
            int coordIdx = restPart.lastIndexOf(",[");
            if (coordIdx >= 0) {
                String coordPart = restPart.substring(coordIdx + 2, restPart.length() - 1);
                // 格式应为 lat@lon
                int at = coordPart.indexOf('@');
                if (at > 0) {
                    String wifiGeoLat = coordPart.substring(0, at);
                    String wifiGeoLon = coordPart.substring(at + 1);
                    p.put("wifiGeoLat", wifiGeoLat);
                    p.put("wifiGeoLon", wifiGeoLon);
                    // 剥离掉末尾的 ",[lat@lon]"
                    restPart = restPart.substring(0, coordIdx);
                }
            }
        }
        p.put("rawGpsPart", gpsPart);
        p.put("rawExtraPart", restPart);

        String valid = null;
        String latRaw = null;
        String lngRaw = null;

        if (gpsPart.length() >= 7) {
            String date = gpsPart.substring(0,6);
            p.put("date", date);
            int pos = 6;
            valid = gpsPart.substring(pos, Math.min(pos+1, gpsPart.length()));
            p.put("valid", valid);
            pos += 1;

            int latEnd = indexOfAny(gpsPart, pos, new char[]{'N','S'});
            if (latEnd > pos) {
                latRaw = gpsPart.substring(pos, latEnd+1);
                p.put("latRaw", latRaw);
                Double lat = convertNMEAToDecimal(latRaw);
                if (lat != null) p.put("lat", String.valueOf(lat));
                pos = latEnd+1;
            }

            int lngEnd = indexOfAny(gpsPart, pos, new char[]{'E','W'});
            if (lngEnd > pos) {
                lngRaw = gpsPart.substring(pos, lngEnd+1);
                p.put("lngRaw", lngRaw);
                Double lng = convertNMEAToDecimal(lngRaw);
                if (lng != null) p.put("lon", String.valueOf(lng));
                pos = lngEnd+1;
            }

            if (pos < gpsPart.length()) {
                String tail = gpsPart.substring(pos);
                Pattern pat = Pattern.compile("^([0-9]+?\\.[0-9]+?)([0-9]{6})(.*)$");
                Matcher m = pat.matcher(tail);
                if (m.find()) {
                    String speed = m.group(1);
                    String gpsTime = m.group(2);
                    String rest = m.group(3);

                    String course = rest;
                    String paramsStr = "";

                    // 首先尝试匹配: course 至少 2 位小数，紧跟恰好 14 位数字（和示例一致）
                    Pattern exactCourseThenParams = Pattern.compile("^([0-9]+\\.[0-9]{2,})([0-9]{14})$");
                    Matcher exactMatched = exactCourseThenParams.matcher(rest);
                    if (exactMatched.find()) {
                        course = exactMatched.group(1);
                        paramsStr = exactMatched.group(2);
                    } else {
                        // 回退：优先查找恰好 14 位的尾部数字串
                        Pattern suf = Pattern.compile("([0-9]{14})$");
                        Matcher ms = suf.matcher(rest);
                        if (ms.find()) {
                            paramsStr = ms.group(1);
                            course = rest.substring(0, rest.length() - paramsStr.length());
                        } else {
                            // 若没有精确 14 位尾串，尝试提取尾部的数字串作为 params（最少 1 位）
                            Pattern suf2 = Pattern.compile("([0-9]+)$");
                            Matcher m2 = suf2.matcher(rest);
                            if (m2.find()) {
                                paramsStr = m2.group(1);
                                course = rest.substring(0, rest.length() - paramsStr.length());
                            }
                        }
                    }

                    course = course == null ? "" : course.trim();
                    paramsStr = paramsStr == null ? "" : paramsStr.trim();

                    p.put("speed", speed);
                    p.put("gpsTime", gpsTime);
                    p.put("course", course);
                    if (!paramsStr.isEmpty() && paramsStr.length() >= 14) {
                        try {
                            p.put("gsm", paramsStr.substring(0,3));
                            p.put("satellite", paramsStr.substring(3,6));
                            p.put("battery", paramsStr.substring(6,9));
                            p.put("reserved", paramsStr.substring(9,10));
                            p.put("arm", paramsStr.substring(10,12));
                            p.put("workMode", paramsStr.substring(12,14));
                        } catch(Exception ex) {
                            p.put("paramsStr", paramsStr);
                        }
                    } else if (!paramsStr.isEmpty()) {
                        p.put("paramsStr", paramsStr);
                    }
                } else {
                    p.put("gpsTail", tail);
                }
            }
        }

        if (!restPart.isEmpty()) {
            String[] restTokens = restPart.split(",", 5);
            if (restTokens.length >= 4) {
                p.put("mcc", restTokens[0].trim());
                p.put("mnc", restTokens[1].trim());
                p.put("lac", restTokens[2].trim());
                p.put("cid", restTokens[3].trim());
            }
            if (restTokens.length >= 5) {
                String wifiRaw = restTokens[4].trim();
                p.put("wifiRaw", wifiRaw);
                String[] groups = wifiRaw.split("&");
                int gi = 0;
                for (String g : groups) {
                    String gg = g.trim();
                    if (gg.isEmpty()) continue;
                    String[] parts = gg.split("\\|");
                    String ssid = parts.length>0?parts[0].trim():"";
                    String mac = parts.length>1?parts[1].trim():"";
                    String rssi = parts.length>2?parts[2].trim():"";
                    String cleanSsid = ssid.replaceAll("[^ -~]", "").trim();
                    if (cleanSsid.isEmpty()) cleanSsid = "wifi"+gi;
                    p.put("wifi."+gi+".ssid", cleanSsid);
                    p.put("wifi."+gi+".mac", mac);
                    p.put("wifi."+gi+".rssi", rssi);
                    gi++;
                }
            }
        }

        boolean gpsValid = "A".equalsIgnoreCase(valid)
                && latRaw != null && lngRaw != null
                && !latRaw.startsWith("0000.0000")
                && !lngRaw.startsWith("00000.0000");
        if (!gpsValid) p.put("useLbsOrWifi","true"); else p.put("useGps","true");

        return p;
    }

    private static int indexOfAny(String s, int from, char[] targets) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            for (char t : targets) if (c == t) return i;
        }
        return -1;
    }

    private static Double convertNMEAToDecimal(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.length() < 2) return null;
        char hemi = raw.charAt(raw.length()-1);
        String body = raw.substring(0, raw.length()-1);
        int dot = body.indexOf('.');
        if (dot <= 0) return null;
        try {
            int beforeDecimal = body.substring(0, dot).length();
            int degLen = beforeDecimal > 4 ? 3 : 2;
            String degStr = body.substring(0, degLen);
            String minStr = body.substring(degLen);
            double deg = Double.parseDouble(degStr);
            double minute = Double.parseDouble(minStr);
            double decimal = deg + (minute/60.0);
            if (hemi == 'S' || hemi == 'W') decimal = -decimal;
            return decimal;
        } catch(Exception ex) {
            return null;
        }
    }
}
