package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TiandituLocationService 单元测试
 */
class TiandituLocationServiceTest {

    private RestTemplate mockRestTemplate;
    private ObjectMapper objectMapper;
    private TiandituLocationService service;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        service = new TiandituLocationService("test-tk", mockRestTemplate, objectMapper);
    }

    @Test
    void regeoAddress_success_returnsFormattedAddress() {
        String json = """
            {
              "status": "0",
              "msg": "ok",
              "result": {
                "formatted_address": "北京市东城区天安门广场"
              }
            }
            """;
        when(mockRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String address = service.regeoAddress(39.9042, 116.4074);
        assertEquals("北京市东城区天安门广场", address);
    }

    @Test
    void regeoAddress_apiErrorStatus_returnsNull() {
        String json = """
            {
              "status": "1",
              "msg": "Key error"
            }
            """;
        when(mockRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String address = service.regeoAddress(39.9042, 116.4074);
        assertNull(address);
    }

    @Test
    void addressToLocation_success_returnsCoordinates() {
        String json = """
            {
              "status": "0",
              "msg": "ok",
              "location": {
                "lon": 116.4074,
                "lat": 39.9042
              }
            }
            """;
        when(mockRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        Map<String, Double> loc = service.addressToLocation("天安门广场");
        assertNotNull(loc);
        assertEquals(39.9042, loc.get("lat"), 0.0001);
        assertEquals(116.4074, loc.get("lng"), 0.0001);
    }

    @Test
    void searchPoi_success_returnsPoiList() {
        String json = """
            {
              "status": "0",
              "pois": [
                {
                  "name": "北京同仁医院",
                  "address": "东交民巷1号",
                  "phone": "010-58266699",
                  "lonlat": "116.4172,39.9008"
                }
              ]
            }
            """;
        when(mockRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        List<Map<String, Object>> list = service.searchPoi("同仁医院", "北京", 10, 1);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("北京同仁医院", list.get(0).get("name"));
    }
}
