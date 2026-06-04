package com.example.demo.service;

import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用高德 WebService 逆地理编码（经纬度 -> 地址）
 */
@Service
public class AmapLocationService {
    private static final Logger log = LoggerFactory.getLogger(AmapLocationService.class);

    private final RestTemplate restTemplate;
    private final String webKey;

    @Autowired
    public AmapLocationService(Environment env) {
        this.restTemplate = new RestTemplate();
        this.webKey = firstNonBlank(
                env.getProperty("AMAP_REST_KEY"),
                env.getProperty("amap.web.key"),
                env.getProperty("AMAP_WEB_KEY"));
    }

    AmapLocationService(String webKey) {
        this.restTemplate = new RestTemplate();
        this.webKey = firstNonBlank(webKey);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 根据经纬度请求高德逆地理编码，返回 formatted_address 或 null
     * @param lat 纬度
     * @param lng 经度
     * @return 地址字符串或 null
     */
    public String regeoAddress(double lat, double lng) {
        if (webKey == null || webKey.isEmpty()) {
            return null;
        }

        // 高德要求 location=lng,lat（经度在前）
        URI uri = UriComponentsBuilder
                .fromUriString("https://restapi.amap.com/v3/geocode/regeo")
                .queryParam("key", webKey)
                .queryParam("location", lng + "," + lat)
                .queryParam("radius", 1000)
                .queryParam("extensions", "all")
                .build()
                .toUri();

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }
            Map<?, ?> body = resp.getBody();
            Object status = body.get("status");
            if (!"1".equals(String.valueOf(status))) {
                log.warn("Amap regeo failed: info={}, infocode={}", body.get("info"), body.get("infocode"));
                return null;
            }
            Object regeocode = body.get("regeocode");
            if (regeocode instanceof Map<?, ?> rmap) {
                Object formatted = rmap.get("formatted_address");
                return formatted != null ? formatted.toString() : null;
            }
            return null;
        } catch (Exception ex) {
            log.warn("Amap regeo request failed: {}", ex.getMessage());
            return null;
        }
    }

    public Map<String, Double> locateByCell(String imei,
                                            String mcc,
                                            String mnc,
                                            String lac,
                                            String cid,
                                            String signal) {
        if (webKey == null || webKey.isEmpty()
                || isBlank(mcc) || isBlank(mnc) || isBlank(lac) || isBlank(cid)) {
            return null;
        }

        String bts = String.join(",",
                mcc.trim(),
                mnc.trim(),
                lac.trim(),
                cid.trim(),
                normalizeSignal(signal));

        URI uri = UriComponentsBuilder
                .fromUriString("https://apilocate.amap.com/position")
                .queryParam("key", webKey)
                .queryParam("accesstype", 0)
                .queryParam("cdma", 0)
                .queryParam("network", "GPRS")
                .queryParam("bts", bts)
                .queryParam("imei", imei == null ? "" : imei)
                .queryParam("platform", "rest")
                .queryParam("output", "json")
                .build()
                .toUri();

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }
            Map<?, ?> body = resp.getBody();
            if (!"1".equals(String.valueOf(body.get("status")))) {
                log.debug("Amap cell locate failed: info={}, infocode={}", body.get("info"), body.get("infocode"));
                return null;
            }

            Object location = null;
            Object result = body.get("result");
            if (result instanceof Map<?, ?> resultMap) {
                location = resultMap.get("location");
            }
            if (location == null) {
                location = body.get("location");
            }
            return parseLocation(location);
        } catch (Exception ex) {
            log.debug("Amap cell locate request failed: {}", ex.getMessage());
            return null;
        }
    }
    
    /**
     * 根据地址请求高德地理编码，返回经纬度对象或 null
     * @param address 地址字符串
     * @return 包含lat和lng的Map，或null
     */
    public Map<String, Double> addressToLocation(String address) {
        System.out.println("addressToLocation called with address: " + address);
        if (webKey == null || webKey.isEmpty() || address == null || address.isEmpty()) {
            System.out.println("Invalid webKey or address");
            return null;
        }
        
        URI uri = UriComponentsBuilder
                .fromUriString("https://restapi.amap.com/v3/geocode/geo")
                .queryParam("key", webKey)
                .queryParam("address", address)
                .build()
                .toUri();
        
        try {
            System.out.println("Calling Amap API with URI: " + uri);
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                System.out.println("Amap API returned non-2xx status or null body: " + resp.getStatusCode());
                return null;
            }
            Map<?, ?> body = resp.getBody();
            System.out.println("Amap API response: " + body);
            Object status = body.get("status");
            if (!"1".equals(String.valueOf(status))) {
                System.out.println("Amap API returned status: " + status);
                return null;
            }
            Object geocodes = body.get("geocodes");
            if (geocodes instanceof List<?> geoList && !geoList.isEmpty()) {
                Object firstGeo = geoList.get(0);
                if (firstGeo instanceof Map<?, ?> geoMap) {
                    Object location = geoMap.get("location");
                    System.out.println("Found location: " + location);
                    if (location != null) {
                        String[] loc = location.toString().split(",");
                        if (loc.length == 2) {
                            Map<String, Double> result = new HashMap<>();
                            // 注意：高德返回的是lng,lat格式
                            result.put("lng", Double.parseDouble(loc[0]));
                            result.put("lat", Double.parseDouble(loc[1]));
                            System.out.println("Returning location: " + result);
                            return result;
                        }
                    }
                }
            }
            System.out.println("No valid geocodes found");
            return null;
        } catch (Exception ex) {
            System.out.println("Exception in addressToLocation: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private static String normalizeSignal(String signal) {
        if (isBlank(signal)) {
            return "null";
        }
        String trimmed = signal.trim();
        try {
            int value = Integer.parseInt(trimmed);
            if (value <= 0) {
                return "null";
            }
            if (value <= 31) {
                int dbm = (2 * value) - 113;
                return String.valueOf(dbm);
            }
        } catch (NumberFormatException ignored) {
            return "null";
        }
        return "null";
    }

    private static Map<String, Double> parseLocation(Object location) {
        if (location == null) {
            return null;
        }
        String[] loc = location.toString().split(",");
        if (loc.length != 2) {
            return null;
        }
        try {
            Map<String, Double> result = new HashMap<>();
            result.put("lng", Double.parseDouble(loc[0]));
            result.put("lat", Double.parseDouble(loc[1]));
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * 根据关键词搜索POI（兴趣点）
     * @param keyword 搜索关键词
     * @param city 城市，可选
     * @param pageSize 每页结果数，默认20
     * @param page 当前页码，默认1
     * @return POI搜索结果列表
     */
    public List<Map<String, Object>> searchPoi(String keyword, String city, Integer pageSize, Integer page) {
        if (webKey == null || webKey.isEmpty() || keyword == null || keyword.isEmpty()) {
            return null;
        }
        
        URI uri = UriComponentsBuilder
                .fromUriString("https://restapi.amap.com/v3/place/text")
                .queryParam("key", webKey)
                .queryParam("keywords", keyword)
                .queryParam("city", city != null ? city : "")
                .queryParam("offset", pageSize != null ? pageSize : 20)
                .queryParam("page", page != null ? page : 1)
                .queryParam("extensions", "all")
                .build()
                .toUri();
        
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(uri, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }
            Map<?, ?> body = resp.getBody();
            Object status = body.get("status");
            if (!"1".equals(String.valueOf(status))) {
                return null;
            }
            Object pois = body.get("pois");
            if (pois instanceof List<?> poiList) {
                List<Map<String, Object>> resultList = new java.util.ArrayList<>();
                for (Object poi : poiList) {
                    if (poi instanceof Map<?, ?> poiMap) {
                        Map<String, Object> poiResult = new HashMap<>();
                        // 提取关键信息
                        poiResult.put("name", poiMap.get("name"));
                        poiResult.put("address", poiMap.get("address"));
                        poiResult.put("tel", poiMap.get("tel"));
                        poiResult.put("category", poiMap.get("type"));
                        
                        // 处理位置信息
                        Object location = poiMap.get("location");
                        if (location != null) {
                            String[] loc = location.toString().split(",");
                            if (loc.length == 2) {
                                Map<String, Double> locMap = new HashMap<>();
                                locMap.put("lng", Double.parseDouble(loc[0]));
                                locMap.put("lat", Double.parseDouble(loc[1]));
                                poiResult.put("location", locMap);
                            }
                        }
                        
                        resultList.add(poiResult);
                    }
                }
                return resultList;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
