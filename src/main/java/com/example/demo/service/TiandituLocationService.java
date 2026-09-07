package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 天地图（Tianditu）地理编码与逆地理编码服务
 * 
 * 官方 Web API 服务：
 * 1. 逆地理编码：http://api.tianditu.gov.cn/geocoder?postStr={'lon':...,'lat':...,'ver':1}&type=geocode&tk=...
 * 2. 地理编码：http://api.tianditu.gov.cn/geocoder?ds={'keyWord':'...'}&tk=...
 * 3. POI 搜索：http://api.tianditu.gov.cn/v2/search?postStr=...&type=query&tk=...
 */
@Service
public class TiandituLocationService {
    private static final Logger log = LoggerFactory.getLogger(TiandituLocationService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String tiandituKey;

    @Autowired
    public TiandituLocationService(Environment env, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        String key = env.getProperty("TIANDITU_KEY");
        if (isBlank(key)) {
            key = env.getProperty("tianditu.key");
        }
        this.tiandituKey = key != null ? key.trim() : "";
    }

    /** 测试构造器 */
    TiandituLocationService(String tiandituKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tiandituKey = tiandituKey != null ? tiandituKey.trim() : "";
    }

    public String getKey() {
        return tiandituKey;
    }

    /**
     * 根据经纬度请求天地图逆地理编码，返回格式化中文地址
     * @param lat 纬度 (CGCS2000 / WGS-84)
     * @param lng 经度 (CGCS2000 / WGS-84)
     * @return 格式化地址或 null
     */
    public String regeoAddress(double lat, double lng) {
        if (isBlank(tiandituKey)) {
            return null;
        }

        String postStr = String.format("{\"lon\":%f,\"lat\":%f,\"ver\":1}", lng, lat);
        URI uri = UriComponentsBuilder
                .fromUriString("http://api.tianditu.gov.cn/geocoder")
                .queryParam("postStr", postStr)
                .queryParam("type", "geocode")
                .queryParam("tk", tiandituKey)
                .build()
                .toUri();

        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(uri, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            String status = root.path("status").asText("");
            if (!"0".equals(status)) {
                log.debug("Tianditu regeo error status: {}, msg: {}", status, root.path("msg").asText(""));
                return null;
            }

            JsonNode result = root.path("result");
            String formattedAddress = result.path("formatted_address").asText(null);
            if (formattedAddress != null && !formattedAddress.isBlank()) {
                return formattedAddress;
            }

            JsonNode addressComponent = result.path("addressComponent");
            String address = addressComponent.path("address").asText(null);
            return (address != null && !address.isBlank()) ? address : null;
        } catch (Exception ex) {
            log.warn("Tianditu regeo request failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 根据中文地址请求天地图地理编码，返回经纬度
     * @param address 地址字符串
     * @return 包含 lat 和 lng 的 Map，或 null
     */
    public Map<String, Double> addressToLocation(String address) {
        if (isBlank(tiandituKey) || isBlank(address)) {
            return null;
        }

        String postStr = String.format("{\"keyWord\":\"%s\"}", address.trim());
        URI uri = UriComponentsBuilder
                .fromUriString("http://api.tianditu.gov.cn/geocoder")
                .queryParam("ds", postStr)
                .queryParam("tk", tiandituKey)
                .build()
                .toUri();

        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(uri, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            String status = root.path("status").asText("");
            if (!"0".equals(status)) {
                return null;
            }

            JsonNode location = root.path("location");
            if (location.hasNonNull("lat") && location.hasNonNull("lon")) {
                Map<String, Double> map = new HashMap<>();
                map.put("lat", location.get("lat").asDouble());
                map.put("lng", location.get("lon").asDouble());
                return map;
            }
            return null;
        } catch (Exception ex) {
            log.warn("Tianditu geocode request failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 根据关键词搜索 POI
     * @param keyword 搜索关键词
     * @param city 城市名称或行政区
     * @param pageSize 每页条数
     * @param page 页码
     * @return POI 列表
     */
    public List<Map<String, Object>> searchPoi(String keyword, String city, Integer pageSize, Integer page) {
        if (isBlank(tiandituKey) || isBlank(keyword)) {
            return null;
        }

        int count = (pageSize != null && pageSize > 0) ? pageSize : 20;
        int start = (page != null && page > 0) ? (page - 1) * count : 0;

        String postStr = String.format("{\"keyWord\":\"%s\",\"level\":11,\"mapBound\":\"-180,-90,180,90\",\"queryType\":1,\"count\":%d,\"start\":%d}",
                keyword.trim(), count, start);

        URI uri = UriComponentsBuilder
                .fromUriString("http://api.tianditu.gov.cn/v2/search")
                .queryParam("postStr", postStr)
                .queryParam("type", "query")
                .queryParam("tk", tiandituKey)
                .build()
                .toUri();

        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(uri, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode pois = root.path("pois");
            if (!pois.isArray()) {
                return null;
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (JsonNode poi : pois) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", poi.path("name").asText(""));
                item.put("address", poi.path("address").asText(""));
                item.put("tel", poi.path("phone").asText(""));
                item.put("category", poi.path("poiType").asText(""));

                String lonlat = poi.path("lonlat").asText("");
                String[] parts = lonlat.split(",");
                if (parts.length == 2) {
                    try {
                        Map<String, Double> loc = new HashMap<>();
                        loc.put("lng", Double.parseDouble(parts[0].trim()));
                        loc.put("lat", Double.parseDouble(parts[1].trim()));
                        item.put("location", loc);
                    } catch (NumberFormatException ignored) {}
                }
                list.add(item);
            }
            return list;
        } catch (Exception ex) {
            log.warn("Tianditu searchPoi request failed: {}", ex.getMessage());
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
